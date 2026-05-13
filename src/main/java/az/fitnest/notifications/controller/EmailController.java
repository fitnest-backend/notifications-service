package az.fitnest.notifications.controller;

import az.fitnest.notifications.dto.BulkEmailRequest;
import az.fitnest.notifications.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
@Tag(name = "Email Service", description = "İstifadəçilərə fərdi və kütləvi elektron poçt göndərilməsi")
public class EmailController {

    private final EmailService emailService;

    @Operation(summary = "Seçilmiş alıcılara kütləvi Email göndərin", description = "Verilmiş email siyahısındakı bütün alıcılara eyni məzmunlu elektron poçt göndərir.")
    @PostMapping("/bulk")
    public ResponseEntity<Void> sendBulkEmail(@Valid @RequestBody BulkEmailRequest request) {
        if (request.emails() != null) {
            for (String email : request.emails()) {
                if (email != null && !email.isBlank()) {
                    emailService.sendSimpleEmail(email, request.subject(), request.body());
                }
            }
        }
        return ResponseEntity.ok().build();
    }
}
