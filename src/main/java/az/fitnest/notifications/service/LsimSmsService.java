package az.fitnest.notifications.service;

import az.fitnest.notifications.exception.SmsBalanceException;
import az.fitnest.notifications.exception.SmsReportException;
import az.fitnest.notifications.exception.SmsSendException;
import az.fitnest.notifications.configuration.LsimSmsProperties;
import az.fitnest.notifications.dto.LsimApiResponse;
import az.fitnest.notifications.dto.LsimReportRequest;
import az.fitnest.notifications.dto.LsimSendSmsRequest;
import az.fitnest.notifications.dto.SmsStatus;
import az.fitnest.notifications.util.LsimHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class LsimSmsService {
    private final WebClient webClient;
    private final LsimSmsProperties properties;

    public Long sendSms(String msisdn, String text, String sender, 
                        Boolean unicode, String scheduled) {
        // 1. Generate key
        String key = LsimHashUtil.generateKey(
            properties.getPassword(),
            properties.getLogin(),
            text,
            msisdn,
            sender
        );

        // 2. Build request
        LsimSendSmsRequest request = LsimSendSmsRequest.builder()
                .login(properties.getLogin())
                .key(key)
                .msisdn(msisdn)
                .text(text)
                .sender(sender)
                .unicode(unicode != null ? unicode : properties.getDefaultUnicode())
                .scheduled(scheduled != null ? scheduled : "NOW")
                .build();

        // 3. Make POST call
        LsimApiResponse response = webClient.post()
                .uri("/quicksms/v1/smssender")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(LsimApiResponse.class)
                .block();  // block for synchronous use; for reactive use .subscribe()

        // 4. Handle response
        if (response == null) {
            throw new SmsSendException("LSIM-dən boş cavab gəldi");
        }
        if (response.getErrorCode() != null && response.getErrorCode() != 0) {
            log.error("LSIM error: {} - {}", response.getErrorCode(), response.getErrorMessage());
            throw new SmsSendException("LSIM xətası: " + response.getErrorMessage());
        }

        // 5. Return transaction ID
        return response.getObj();
    }

    // convenience overload
    public Long sendSms(String msisdn, String text) {
        return sendSms(msisdn, text, properties.getDefaultSender(), 
                       properties.getDefaultUnicode(), "NOW");
    }
    public Integer checkBalance() {
        String key = LsimHashUtil.generateBalanceKey(properties.getPassword(), properties.getLogin());

        String url = "/quicksms/v1/balance?login={login}&key={key}";
        LsimApiResponse response = webClient.get()
                .uri(url, properties.getLogin(), key)
                .retrieve()
                .bodyToMono(LsimApiResponse.class)
                .block();

        if (response == null || (response.getErrorCode() != null && response.getErrorCode() != 0)) {
            String errorMsg = response != null ? 
                String.format("LSIM error %d: %s", response.getErrorCode(), response.getErrorMessage()) : 
                "no response";
            log.error("Balance check failed: {}", errorMsg);
            throw new SmsBalanceException("Balansın yoxlanılması uğursuz oldu: " + errorMsg);
        }
        return response.getObj() != null ? response.getObj().intValue() : 0;
    }


    public class SmsLengthValidator {
        public static boolean isWithinLimit(String text, boolean unicode) {
            int maxChars;
            if (unicode) {
                if (text.length() <= 70) maxChars = 70;
                else if (text.length() <= 134) maxChars = 134;
                    // ... add more segments if your logic supports splitting
                else maxChars = 603; // 9 segments
            } else {
                if (text.length() <= 160) maxChars = 160;
                else if (text.length() <= 306) maxChars = 306;
                else maxChars = 1377; // 9 segments
            }
            return text.length() <= maxChars;
        }
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
            throw new SmsReportException("LSIM-dən boş cavab gəldi");
        }

        if (response.getErrorCode() != null && response.getErrorCode() != 0) {
            throw new SmsReportException("Hesabat uğursuz oldu: " + response.getErrorMessage(),
                    response.getErrorCode());
        }

        if (response.getObj() == null) {
            throw new SmsReportException("LSIM cavabında status kodu yoxdur");
        }

        Integer statusCode = response.getObj().intValue();  // 100-109
        SmsStatus status = SmsStatus.fromCode(statusCode);
        if (status == null) {
            throw new SmsReportException("Naməlum status kodu: " + statusCode);
        }
        return status;
    }
}