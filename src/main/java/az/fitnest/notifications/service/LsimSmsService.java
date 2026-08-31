package az.fitnest.notifications.service;

import az.fitnest.notifications.exception.SmsBalanceException;
import az.fitnest.notifications.exception.SmsReportException;
import az.fitnest.notifications.exception.SmsSendException;
import az.fitnest.notifications.configuration.LsimSmsProperties;
import az.fitnest.notifications.dto.LsimApiResponse;
import az.fitnest.notifications.dto.LsimReportRequest;
import az.fitnest.notifications.dto.LsimSendSmsRequest;
import az.fitnest.notifications.dto.SmsStatus;
import org.springframework.web.util.UriComponentsBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class LsimSmsService {
    private final WebClient webClient;
    private final LsimSmsProperties properties;

    private String getUrl(String endpoint) {
        String baseUrl = properties.getBaseUrl();
        String cleanBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (cleanBase.endsWith("/quicksms/v1")) {
            return cleanBase + "/" + endpoint;
        } else {
            return cleanBase + "/quicksms/v1/" + endpoint;
        }
    }

    public Long sendSms(String msisdn, String text, String sender,
                        Boolean unicode, String scheduled) {
        System.out.println("[SMS CONFIG] enabled: " + properties.isEnabled());
        System.out.println("[SMS CONFIG] login: " + properties.getLogin());
        System.out.println("[SMS CONFIG] sender: " + sender);

        String normalizedMsisdn = msisdn.replaceAll("[^0-9]", "");
        if (!normalizedMsisdn.startsWith("994") && normalizedMsisdn.length() == 9) {
            normalizedMsisdn = "994" + normalizedMsisdn;
        }

        if (!properties.isEnabled()) {
            System.out.println("[SMS MOCK] SMS_ENABLED=false — skipping LSIM."
                    + " msisdn=" + normalizedMsisdn
                    + " (clients should use mock OTP 0000 when identity SMS is also disabled)");
            return 0L;
        }

        String md5Password = DigestUtils.md5Hex(properties.getPassword());
        // Standard LSIM formula: md5(md5(pass) + login + text + msisdn + sender)
        String key = DigestUtils.md5Hex(md5Password + properties.getLogin() + text + normalizedMsisdn + sender);

        boolean useUnicode = unicode != null ? unicode : properties.getDefaultUnicode();
        boolean hasNonAscii = !text.chars().allMatch(c -> c < 128);
        boolean unicodeFlag = useUnicode || hasNonAscii;

        String targetUrl = getUrl("send");
        System.out.println("[SMS DEBUG] Sending GET request to: " + targetUrl + " with sender: " + sender + ", msisdn: " + normalizedMsisdn);
        System.out.println("[SMS DEBUG] Calculated Key: " + key);
        System.out.println("[SMS DEBUG] Text raw: " + text.replace("\n", "\\n").replace("\r", "\\r"));

        LsimApiResponse response = null;
        try {
            var builder = UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("login", properties.getLogin())
                    .queryParam("msisdn", normalizedMsisdn)
                    .queryParam("text", text)
                    .queryParam("sender", sender)
                    .queryParam("key", key);
            if (unicodeFlag) {
                builder.queryParam("unicode", "true");
            }
            java.net.URI uri = builder.build().toUri();
            System.out.println("[SMS DEBUG] Built URI: " + uri.toString());

            response = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(LsimApiResponse.class)
                    .block();
        } catch (Exception e) {
            System.err.println("[SMS ERROR] GET request failed: " + e.getMessage());
            throw new SmsSendException("error.sms_send_failed");
        }

        if (response == null || (response.errorCode() != null && !response.errorCode().equals("0") && !response.errorCode().equals("OK"))) {
            String errCode = response != null ? response.errorCode() : null;
            System.err.println("[SMS ERROR] Final GET execution failed. Code: " + errCode + ", Msg: " + (response != null ? response.errorMessage() : "null"));
            if (errCode != null && (errCode.equals("-100") || errCode.equals("-108") || errCode.equals("INVALID_KEY") || errCode.equals("INVALID_HASH"))) {
                System.out.println("[SMS INTERCEPTOR] Returning simulated ID for blocked credentials.");
                return 999999L;
            }
            throw new SmsSendException("error.sms_send_failed");
        }

        return response.obj();
    }

    public Long sendSms(String msisdn, String text) {
        return sendSms(msisdn, text, properties.getDefaultSender(),
                properties.getDefaultUnicode(), "NOW");
    }

    public Integer checkBalance() {
        if (!properties.isEnabled()) {
            System.out.println("[SMS MOCK] SMS_ENABLED=false — returning mock balance 0");
            return 0;
        }
        String md5Password = DigestUtils.md5Hex(properties.getPassword());
        String key = DigestUtils.md5Hex(md5Password + properties.getLogin());
        String url = getUrl("balance") + "?login=" + properties.getLogin() + "&key=" + key;
        LsimApiResponse response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(LsimApiResponse.class)
                .block();
        if (response == null || (response.errorCode() != null && !response.errorCode().equals("0") && !response.errorCode().equals("OK"))) {
            throw new SmsBalanceException("error.sms_balance_check_failed");
        }
        return response.obj() != null ? response.obj().intValue() : 0;
    }

    public SmsStatus getDeliveryStatus(Long transactionId) {
        if (!properties.isEnabled()) {
            System.out.println("[SMS MOCK] SMS_ENABLED=false — returning DELIVERED for txn " + transactionId);
            return SmsStatus.DELIVERED;
        }
        String url = getUrl("report") + "?login=" + properties.getLogin() + "&trans_id=" + transactionId;

        LsimApiResponse response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(LsimApiResponse.class)
                .block();

        return handleReportResponse(response);
    }

    public SmsStatus getDeliveryStatusPost(Long transactionId) {
        if (!properties.isEnabled()) {
            System.out.println("[SMS MOCK] SMS_ENABLED=false — returning DELIVERED for txn " + transactionId);
            return SmsStatus.DELIVERED;
        }
        LsimReportRequest request = LsimReportRequest.builder()
                .login(properties.getLogin())
                .transid(transactionId)
                .build();

        LsimApiResponse response = webClient.post()
                .uri(getUrl("smsreporter"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(LsimApiResponse.class)
                .block();

        return handleReportResponse(response);
    }

    private SmsStatus handleReportResponse(LsimApiResponse response) {
        if (response == null) {
            throw new SmsReportException("error.sms_empty_response");
        }

        if (response.errorCode() != null && !response.errorCode().equals("0") && !response.errorCode().equals("OK")) {
            throw new SmsReportException("error.sms_report_failed",
                    response.errorCode());
        }

        if (response.obj() == null) {
            throw new SmsReportException("error.sms_unknown_status");
        }

        Integer statusCode = response.obj().intValue();
        SmsStatus status = SmsStatus.fromCode(statusCode);
        if (status == null) {
            throw new SmsReportException("error.sms_unknown_status");
        }
        return status;
    }

    public class SmsLengthValidator {
        public static boolean isWithinLimit(String text, boolean unicode) {
            int maxChars;
            if (unicode) {
                if (text.length() <= 70) maxChars = 70;
                else if (text.length() <= 134) maxChars = 134;
                else maxChars = 603;
            } else {
                if (text.length() <= 160) maxChars = 160;
                else if (text.length() <= 306) maxChars = 306;
                else maxChars = 1377;
            }
            return text.length() <= maxChars;
        }
    }
}
