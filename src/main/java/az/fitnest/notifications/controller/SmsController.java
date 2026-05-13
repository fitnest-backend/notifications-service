package az.fitnest.notifications.controller;

import az.fitnest.notifications.service.LsimSmsService;
import az.fitnest.notifications.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sms")
@Tag(name = "SMS Service", description = "SMS göndərilməsi, balansın yoxlanılması, çatdırılma hesabatlarının alınması")
@RequiredArgsConstructor
public class SmsController {

    private final LsimSmsService lsimSmsService;

    @PostMapping("/send")
    @Operation(
            summary = "SMS mesajı göndərin",
            description = "Göstərilən alıcıya SMS mesajı göndərir. Mesaj gələcək çatdırılma üçün planlaşdırıla bilər və ya dərhal göndərilə bilər. Beynəlxalq simvollar və emojilər üçün Unicode-u dəstəkləyir. Çatdırılma statusunu izləmək üçün tranzaksiya ID-sini qaytarır."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "SMS uğurla göndərildi",
                    content = @Content(schema = @Schema(implementation = SendSmsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Yanlış sorğu məlumatı və ya validasiya xətası",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Daxili server xətası və ya SMS provayderinin xətası",
                    content = @Content
            )
    })
    public ResponseEntity<SendSmsResponse> sendSms(@Valid @RequestBody SendSmsRequest request) {
        Long transactionId = lsimSmsService.sendSms(
                request.msisdn(),
                request.text(),
                request.sender(),
                request.unicode(),
                request.scheduled()
        );
        return ResponseEntity.ok(new SendSmsResponse(transactionId));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Çoxlu alıcıya SMS göndərin", description = "Verilmiş nömrələr siyahısına eyni məzmunlu SMS göndərir.")
    public ResponseEntity<java.util.List<SendSmsResponse>> sendBulkSms(@Valid @RequestBody az.fitnest.notifications.dto.BulkSmsRequest request) {
        java.util.List<SendSmsResponse> responses = new java.util.ArrayList<>();
        if (request.phoneNumbers() != null) {
            for (String phone : request.phoneNumbers()) {
                if (phone != null && !phone.isBlank()) {
                    try {
                        Long txId = lsimSmsService.sendSms(phone, request.text(), null, true, null);
                        responses.add(new SendSmsResponse(txId));
                    } catch (Exception e) {
                        // ignore failures for individual numbers to continue dispatching rest
                    }
                }
            }
        }
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/balance")
    @Operation(
            summary = "Qalan SMS balansını yoxlayın",
            description = "Mesaj göndərmək üçün mövcud olan qalan SMS kreditlərinin sayını əldə edir. Bu balans LSIM SMS provayderi tərəfindən idarə olunur və mesajın uzunluğuna və növünə əsasən çıxılır."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Balans uğurla əldə edildi",
                    content = @Content(schema = @Schema(implementation = BalanceResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Daxili server xətası və ya provayderlə əlaqə xətası",
                    content = @Content
            )
    })
    public ResponseEntity<BalanceResponse> checkBalance() {
        Integer balance = lsimSmsService.checkBalance();
        return ResponseEntity.ok(new BalanceResponse(balance));
    }

    @GetMapping("/report/{transactionId}")
    @Operation(
            summary = "Göndərilmiş SMS-in çatdırılma statusunu əldə edin",
            description = "Tranzaksiya ID-si vasitəsilə əvvəllər göndərilmiş SMS-in çatdırılma statusunu əldə edir. Status kodları mesajın çatdırıldığını, uğursuz olduğunu və ya hələ də prosesdə olduğunu göstərir."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Çatdırılma statusu uğurla əldə edildi",
                    content = @Content(schema = @Schema(implementation = ReportResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Tranzaksiya ID-si tapılmadı",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Daxili server xətası və ya provayderlə əlaqə xətası",
                    content = @Content
            )
    })
    public ResponseEntity<ReportResponse> getReport(@PathVariable Long transactionId) {
        SmsStatus status = lsimSmsService.getDeliveryStatus(transactionId);
        return ResponseEntity.ok(new ReportResponse(transactionId, status));
    }
}
