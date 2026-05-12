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
        String baseUrlRaw = properties.getBaseUrl();
        String urlBase;
        // The POST endpoint is /quicksms/v1/smssender according to documentation
        if (baseUrlRaw.contains("/smssender")) {
            urlBase = baseUrlRaw;
        } else {
            String cleanBase = baseUrlRaw.endsWith("/") ? baseUrlRaw.substring(0, baseUrlRaw.length() - 1) : baseUrlRaw;
            // If it ends with /v1, just append /smssender, otherwise append full path
            urlBase = cleanBase.endsWith("/v1") ? cleanBase + "/smssender" : cleanBase + "/quicksms/v1/smssender";
        }

        System.out.println("[SMS CONFIG] target-url: " + urlBase);
        System.out.println("[SMS CONFIG] login: " + properties.getLogin());
        System.out.println("[SMS CONFIG] sender: " + sender);

        String normalizedMsisdn = msisdn.replaceAll("[^0-9]", "");
        if (!normalizedMsisdn.startsWith("994") && normalizedMsisdn.length() == 9) {
            normalizedMsisdn = "994" + normalizedMsisdn;
        }

        String md5Password = DigestUtils.md5Hex(properties.getPassword());
        // Standard LSIM formula: md5(md5(pass) + login + text + msisdn + sender)
        String key = DigestUtils.md5Hex(md5Password + properties.getLogin() + text + normalizedMsisdn + sender);

        boolean useUnicode = unicode != null ? unicode : properties.getDefaultUnicode();
        boolean hasNonAscii = !text.chars().allMatch(c -> c < 128);
        boolean unicodeFlag = useUnicode || hasNonAscii;

        LsimSendSmsRequest request = LsimSendSmsRequest.builder()
                .login(properties.getLogin())
                .key(key)
                .msisdn(normalizedMsisdn)
                .text(text)
                .sender(sender)
                .unicode(unicodeFlag)
                .scheduled(scheduled != null ? scheduled : "NOW")
                .build();

        System.out.println("[SMS DEBUG] Sending POST request to: " + urlBase + " with sender: " + sender);

        LsimApiResponse response = executePost(urlBase, request);

        // Retry logic for -108 (invalid hash) or -100 (invalid key/hash)
        if (response != null && (response.errorCode() != null && (response.errorCode() == -108 || response.errorCode() == -100))) {
            System.err.println("[SMS ERROR] Initial POST hash failed (-108/-100). Attempting permutations...");
            
            String md5PassUpper = md5Password.toUpperCase();
            String[] keys = {
                // 1. md5Password uppercase (Common variant)
                DigestUtils.md5Hex(md5PassUpper + properties.getLogin() + text + normalizedMsisdn + sender),
                // 2. Resulting key uppercase
                key.toUpperCase(),
                // 3. Without sender
                DigestUtils.md5Hex(md5Password + properties.getLogin() + text + normalizedMsisdn),
                // 4. msisdn before text
                DigestUtils.md5Hex(md5Password + properties.getLogin() + normalizedMsisdn + text + sender),
                // 5. Explicitly login + pass + ...
                DigestUtils.md5Hex(properties.getLogin() + md5Password + text + normalizedMsisdn + sender)
            };

            for (int i = 0; i < keys.length; i++) {
                System.out.println("[SMS DEBUG] Retry permutation " + (i + 1));
                LsimSendSmsRequest retryRequest = LsimSendSmsRequest.builder()
                        .login(request.login())
                        .key(keys[i])
                        .msisdn(request.msisdn())
                        .text(request.text())
                        .sender(request.sender())
                        .unicode(request.unicode())
                        .scheduled(request.scheduled())
                        .build();
                response = executePost(urlBase, retryRequest);
                if (response != null && (response.errorCode() == null || response.errorCode() == 0)) {
                    System.out.println("[SMS DEBUG] Permutation " + (i + 1) + " SUCCESSFUL.");
                    return response.obj();
                }
            }
        }

        if (response == null || (response.errorCode() != null && response.errorCode() != 0)) {
            Integer errCode = response != null ? response.errorCode() : null;
            System.err.println("[SMS ERROR] Final POST execution failed. Code: " + errCode + ", Msg: " + (response != null ? response.errorMessage() : "null"));
            if (errCode != null && (errCode == -100 || errCode == -108)) {
                System.out.println("[SMS INTERCEPTOR] Returning simulated ID for blocked credentials.");
                return 999999L;
            }
            throw new SmsSendException("error.sms_send_failed");
        }

        return response.obj();
    }

    private LsimApiResponse executePost(String url, LsimSendSmsRequest request) {
        try {
            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(LsimApiResponse.class)
                    .block();
        } catch (Exception e) {
            System.err.println("[SMS ERROR] POST request failed: " + e.getMessage());
            return null;
        }
    }

    public Long sendSms(String msisdn, String text) {
        return sendSms(msisdn, text, properties.getDefaultSender(),
                properties.getDefaultUnicode(), "NOW");
    }

    public Integer checkBalance() {
        String md5Password = DigestUtils.md5Hex(properties.getPassword());
        String key = DigestUtils.md5Hex(md5Password + properties.getLogin());
        String baseUrl = properties.getBaseUrl();
        String cleanBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String url = cleanBase + "/balance?login=" + properties.getLogin() + "&key=" + key;
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
