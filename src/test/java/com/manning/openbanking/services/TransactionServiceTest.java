package com.manning.openbanking.services;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.manning.openbanking.OpenbankingApplication;
import com.manning.openbanking.entities.Transaction;
import com.manning.openbanking.repositories.MerchantDetailsRepository;
import com.manning.openbanking.repositories.TransactionApiClient;
import com.manning.openbanking.repositories.TransactionRepository;



@SpringBootTest(classes = {OpenbankingApplication.class})
public class TransactionServiceTest {

     @Mock
    private TransactionApiClient transactionApiClient;

    @Mock
    private MerchantDetailsRepository merchantDetailsRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    // Datos de prueba
    private Transaction mockTransaction;

    @BeforeEach
    public void setUp() {
        mockTransaction = new Transaction();
        mockTransaction.setAccountNumber(123456789);
        mockTransaction.setMerchantName("Merchant A");
    }

    @DisplayName("Test - Cuando hay transacciones en la base de datos")
    @Test
    public void testFindAllByAccountNumber_WithTransactions() throws Exception {
        // Simulamos que existen transacciones en la base de datos
        when(transactionRepository.findAllByAccountNumber(any()))
                .thenReturn(List.of(mockTransaction)); 

        // Simulamos que el repositorio de detalles de mercader retorna un logo
        when(merchantDetailsRepository.findMerchantLogo(any()))
                .thenReturn(Optional.of("some_logo_url"));

        // Llamamos al método de servicio
        List<Transaction> transactions = transactionService.findAllByAccountNumber(1234567);

        // Verificamos el tamaño de la lista
        assertEquals(1, transactions.size());
        assertEquals("some_logo_url", transactions.get(0).getMerchantLogo());

        // Verificamos que los mocks fueron llamados
        verify(transactionRepository, times(1)).findAllByAccountNumber(1234567);
        verify(merchantDetailsRepository, times(1)).findMerchantLogo("Merchant A");
    }



    // @DisplayName("Test - Fallback invocado cuando ocurre un error")
    // @Test
    // public void testHandleTransactionFallback() throws Exception {
    //     // Simulamos que hay un error en la obtención de transacciones de la base de datos
    //     when(transactionRepository.findAllByAccountNumber(any()))
    //             .thenThrow(new RuntimeException("Error en la base de datos"));

    //     // Simulamos que la API de transacciones devuelve datos
    //     when(transactionApiClient.findAllByAccountNumber(any()))
    //             .thenReturn(List.of(mockTransaction));

    //     // Llamamos al método de servicio
    //     List<Transaction> transactions = transactionService.findAllByAccountNumber(any());

    //     // Verificamos que el fallback fue llamado correctamente
    //     assertEquals(1, transactions.size());

    //     // Verificamos que el pollByAccountNumber fue invocado para manejar el fallback
    //     verify(transactionRepository, times(1)).findAllByAccountNumber(123456789);
    // }

    @DisplayName("Test - Polling de transacciones para todas las cuentas")
    @Test
    public void testPollTransactionsForAllAccounts() throws Exception {
        // Simulamos que existen números de cuenta en la base de datos
        when(transactionRepository.findAllAccountNumbers())
                .thenReturn(List.of(1234567));

        // Simulamos que la API de transacciones devuelve datos
        when(transactionApiClient.findAllByAccountNumber(any()))
                .thenReturn(List.of(mockTransaction));

        // Llamamos al método de polling
        transactionService.pollTransactionsForAllAccounts();

        // Verificamos que se realizó el polling para la cuenta
        verify(transactionRepository, times(1)).findAllAccountNumbers();
        verify(transactionApiClient, times(1)).findAllByAccountNumber(1234567);
    }
}
