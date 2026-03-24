package org.example.sdpclient.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Optional;

import org.example.sdpclient.dto.AdminLogin;
import org.example.sdpclient.dto.AdminRegisterRequest;
import org.example.sdpclient.entity.Admin;
import org.example.sdpclient.repository.AdminRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminLoginServiceTest {

    @Mock
    private AdminRepository repo;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private AdminLoginService service;

    // -------------------------
    // login() tests
    // -------------------------

    @Test
    void login_shouldReturnOkFalse_whenUserNotFound() {
        AdminLogin req = mock(AdminLogin.class);
        when(req.getUsername()).thenReturn("missing");

        when(repo.findByUsernameIgnoreCase("missing")).thenReturn(Optional.empty());

        Map<String, Object> result = service.login(req);

        assertEquals(false, result.get("ok"));
        assertEquals(1, result.size());

        verify(repo).findByUsernameIgnoreCase("missing");
        verifyNoInteractions(encoder);
    }

    @Test
    void login_shouldReturnOkFalse_whenPasswordDoesNotMatch() {
        AdminLogin req = mock(AdminLogin.class);
        when(req.getUsername()).thenReturn("admin");
        when(req.getPassword()).thenReturn("wrongpw");

        Admin admin = new Admin();
        admin.setUsername("admin");
        admin.setPasswordHash("hashed");
        admin.setRoot(true);

        when(repo.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
        when(encoder.matches("wrongpw", "hashed")).thenReturn(false);

        Map<String, Object> result = service.login(req);

        assertEquals(false, result.get("ok"));
        assertEquals(1, result.size());
        verify(repo).findByUsernameIgnoreCase("admin");
        verify(encoder).matches("wrongpw", "hashed");
    }

    @Test
    void login_shouldReturnOkTrueAndUserInfo_whenPasswordMatches() {
        AdminLogin req = mock(AdminLogin.class);
        when(req.getUsername()).thenReturn("admin");
        when(req.getPassword()).thenReturn("pw");

        Admin admin = new Admin();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPasswordHash("hashed");
        admin.setRoot(false);

        when(repo.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
        when(encoder.matches("pw", "hashed")).thenReturn(true);

        Map<String, Object> result = service.login(req);

        assertEquals(true, result.get("ok"));
        assertEquals("admin", result.get("username"));
        assertEquals(1L, result.get("id"));
        assertEquals(false, result.get("root"));

        verify(repo).findByUsernameIgnoreCase("admin");
        verify(encoder).matches("pw", "hashed");
    }

    // -------------------------
    // register() tests
    // -------------------------

    @Test
    void register_shouldFail_whenAnyFieldMissingOrBlank() {
        AdminRegisterRequest req = mock(AdminRegisterRequest.class);

        // only stub what is needed for this path
        when(req.getUsername()).thenReturn("   ");

        Map<String, Object> result = service.register(req, 1L, "testAdmin");

        assertEquals(false, result.get("ok"));
        assertEquals("All fields are required", result.get("message"));

        verifyNoInteractions(repo);
        verifyNoInteractions(encoder);
    }

    @Test
    void register_shouldFail_whenUsernameAlreadyExists() {
        AdminRegisterRequest req = mock(AdminRegisterRequest.class);
        when(req.getUsername()).thenReturn(" admin ");
        when(req.getPassword()).thenReturn("pw");
        when(req.getFirstName()).thenReturn("John");
        when(req.getLastName()).thenReturn("Doe");
        when(req.getEmail()).thenReturn("admin@test.com");
        when(req.getPhone()).thenReturn("+447700900001");

        when(repo.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(new Admin()));

        Map<String, Object> result = service.register(req, 1L, "testAdmin");

        assertEquals(false, result.get("ok"));
        assertEquals("Username already exists", result.get("message"));

        verify(repo).findByUsernameIgnoreCase("admin");
        verify(repo, never()).save(any(Admin.class));
        verifyNoInteractions(encoder);
    }

    @Test
    void register_shouldSaveAdminAndReturnOkTrue_whenValid() {
        AdminRegisterRequest req = mock(AdminRegisterRequest.class);
        when(req.getUsername()).thenReturn(" admin ");
        when(req.getPassword()).thenReturn("pw");
        when(req.getFirstName()).thenReturn(" John ");
        when(req.getLastName()).thenReturn(" Doe ");
        when(req.getEmail()).thenReturn(" admin@test.com ");
        when(req.getPhone()).thenReturn(" +447700900001 ");

        when(repo.findByUsernameIgnoreCase("admin")).thenReturn(Optional.empty());
        when(encoder.encode("pw")).thenReturn("hashedPw");

        Map<String, Object> result = service.register(req, 1L, "testAdmin");

        assertEquals(true, result.get("ok"));
        assertEquals("admin", result.get("username"));

        // Capture what got saved
        ArgumentCaptor<Admin> captor = ArgumentCaptor.forClass(Admin.class);
        verify(repo).save(captor.capture());

        Admin saved = captor.getValue();
        assertEquals("admin", saved.getUsername());
        assertEquals("hashedPw", saved.getPasswordHash());
        assertEquals("John", saved.getFirstName());
        assertEquals("Doe", saved.getLastName());
        assertEquals("admin@test.com", saved.getEmail());
        assertEquals("+447700900001", saved.getPhone());

        verify(repo).findByUsernameIgnoreCase("admin");
        verify(encoder).encode("pw");
    }
}

