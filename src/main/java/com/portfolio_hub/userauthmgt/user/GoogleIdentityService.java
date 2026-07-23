package com.portfolio_hub.userauthmgt.user;


import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.portfolio_hub.utils.exception.InvalidOperationException;
import com.portfolio_hub.utils.exception.UnauthorizedException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleIdentityService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleIdentityService(
            @Value("${application.google.client-id:}") String clientId
    ) {
        this.verifier = clientId == null || clientId.isBlank()
                ? null
                : new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(Collections.singletonList(clientId.trim()))
                .build();
    }

    public GoogleProfile verify(String credential) {
        if (verifier == null) {
            throw new InvalidOperationException(
                    "Google authentication is not configured"
            );
        }

        try {
            GoogleIdToken idToken = verifier.verify(credential);

            if (idToken == null) {
                throw new UnauthorizedException("Google authentication failed");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String subject = payload.getSubject();
            String email = payload.getEmail();
            Boolean emailVerified = payload.getEmailVerified();
            Object nameClaim = payload.get("name");
            Object pictureClaim = payload.get("picture");

            if (
                    subject == null ||
                            subject.isBlank() ||
                            email == null ||
                            email.isBlank() ||
                            !Boolean.TRUE.equals(emailVerified)
            ) {
                throw new UnauthorizedException(
                        "Google did not return a verified account"
                );
            }

            String fullName = nameClaim == null
                    ? email.substring(0, email.indexOf('@'))
                    : nameClaim.toString().trim();

            if (fullName.isBlank()) {
                fullName = email.substring(0, email.indexOf('@'));
            }

            return new GoogleProfile(
                    subject,
                    email.trim().toLowerCase(),
                    fullName,
                    pictureClaim == null ? null : pictureClaim.toString()
            );
        } catch (UnauthorizedException exception) {
            throw exception;
        } catch (GeneralSecurityException | IOException exception) {
            throw new UnauthorizedException("Google authentication failed");
        }
    }

    public record GoogleProfile(
            String subject,
            String email,
            String fullName,
            String pictureUrl
    ) {}
}