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

    public Long sendSms(String msisdn, String text, String sender,
                        Boolean unicode, String scheduled) {
        System.out.println("[SMS CONFIG] base-url: " + properties.getBaseUrl());
        System.out.println("[SMS CONFIG] login: " + properties.getLogin());
        System.out.println("[SMS CONFIG] default-sender: " + properties.getDefaultSender());
        String maskedPassword = properties.getPassword() == null ? null : properties.getPassword().replaceAll(".", "*");
        System.out.println("[SMS CONFIG] password: " + maskedPassword);

        String normalizedMsisdn = msisdn.replaceAll("[^0-9]", "");
        if (!normalizedMsisdn.startsWith("994")) {
            throw new SmsSendException("Phone number must start with country code 994");
        }

        String textParam = text;
        String md5Password = DigestUtils.md5Hex(properties.getPassword());
        String key = DigestUtils.md5Hex(md5Password + properties.getLogin() + textParam + normalizedMsisdn + sender);

        boolean useUnicode = unicode != null ? unicode : properties.getDefaultUnicode();
        boolean hasNonAscii = !text.chars().allMatch(c -> c < 128);
        boolean unicodeFlag = useUnicode || hasNonAscii;

        String baseUrl = properties.getBaseUrl();
        if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        String urlBase = baseUrl + "/send";

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(urlBase)
                .queryParam("login", properties.getLogin())
                .queryParam("msisdn", normalizedMsisdn)
                .queryParam("text", textParam)
                .queryParam("sender", sender)
                .queryParam("key", key)
                .queryParam("unicode", unicodeFlag);

        String url = builder.toUriString();

        System.out.println("[SMS DEBUG] Sending SMS request: " + url.replace(key, "[KEY]") + " (sender: " + sender + ")");

        LsimApiResponse response;
        try {
            response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(LsimApiResponse.class)
                    .block();
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.Forbidden e) {
            System.err.println("[SMS ERROR] 403 Forbidden from sendsms.az. Check IP whitelisting, credentials, and sender name.");
            throw new SmsSendException("403 Forbidden from SMS provider. Check IP whitelisting, credentials, and sender name.");
        }

        if (response == null) {
            throw new SmsSendException("error.sms_empty_response");
        }
        if (response.errorCode() != null && response.errorCode() != 0) {
            if (response.errorCode() == -109) {
                response = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(LsimApiResponse.class)
                        .block();
                if (response == null || (response.errorCode() != null && response.errorCode() != 0)) {
                    throw new SmsSendException("error.sms_send_failed");
                }
            } else {
                throw new SmsSendException("error.sms_send_failed");
            }
        }

        return response.obj();
    }

    public Long sendSms(String msisdn, String text) {
        return sendSms(msisdn, text, properties.getDefaultSender(),
                properties.getDefaultUnicode(), "NOW");
    }

    public Integer checkBalance() {
        String md5Password = DigestUtils.md5Hex(properties.getPassword());
        String key = DigestUtils.md5Hex(md5Password + properties.getLogin());
        String url = properties.getBaseUrl() + "/balance?login=" + properties.getLogin() + "&key=" + key;
        LsimApiResponse response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(LsimApiResponse.class)
                .block();
        if (response == null || (response.errorCode() != null && response.errorCode() != 0)) {
            throw new SmsBalanceException("error.sms_balance_check_failed");
        }
        return response.obj() != null ? response.obj().intValue() : 0;
    }

    public SmsStatus getDeliveryStatus(Long transactionId) {
        String url = "/quicksms/v1/report?login={login}&trans_id={trans_id}";

        LsimApiResponse response = webClient.get()
                .uri(url, properties.getLogin(), transactionId)
                .retrieve()
                .bodyToMono(LsimApiResponse.class)
                .block();

        return handleReportResponse(response);
    }

    public SmsStatus getDeliveryStatusPost(Long transactionId) {
        LsimReportRequest request = LsimReportRequest.builder()
                .login(properties.getLogin())
                .transid(transactionId)
                .build();

        LsimApiResponse response = webClient.post()
                .uri("/quicksms/v1/smsreporter")
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

        if (response.errorCode() != null && response.errorCode() != 0) {
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
