package com.sentinelcore.backend.service;

import com.sentinelcore.backend.dto.AlertDTO;
import com.sentinelcore.backend.entity.Alert;
import com.sentinelcore.backend.exception.ResourceNotFoundException;
import com.sentinelcore.backend.repository.AlertRepository;
import com.sentinelcore.backend.repository.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AlertService alertService;

    private Alert alert;

    @BeforeEach
    void setUp() {
        alert = Alert.builder()
                .id(1L)
                .title("Unauthorized Login")
                .severity("HIGH")
                .status("OPEN")
                .category("Auth")
                .sourceIp("192.168.1.10")
                .build();
    }

    @Test
    void testGetAlertById_Success() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        AlertDTO result = alertService.getAlertById(1L);

        assertNotNull(result);
        assertEquals("Unauthorized Login", result.getTitle());
        assertEquals("HIGH", result.getSeverity());
        verify(alertRepository, times(1)).findById(1L);
    }

    @Test
    void testGetAlertById_NotFound() {
        when(alertRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> alertService.getAlertById(99L));
        verify(alertRepository, times(1)).findById(99L);
    }
}
