package com.manning.openbanking.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.stereotype.Service;

import com.manning.openbanking.entities.Transaction;
import com.manning.openbanking.repositories.MerchantDetailsRepository;
import com.manning.openbanking.repositories.TransactionApiClient;
import com.manning.openbanking.repositories.TransactionRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TransactionService {

    private final TransactionApiClient transactionApiClient;
    private final MerchantDetailsRepository merchantDetailsRepository;
    private final TransactionRepository transactionRepository;
    
    public TransactionService(
        final TransactionApiClient transactionApiClient,
        final MerchantDetailsRepository merchantDetailsRepository,
        final TransactionRepository transactionRepository) {
        this.transactionApiClient = transactionApiClient;
        this.merchantDetailsRepository = merchantDetailsRepository;
        this.transactionRepository = transactionRepository;
}


    //@PostFilter(value = "hasAuthority(filterObject.accountNumber)")
    @CircuitBreaker(name = "transactionService", fallbackMethod = "handleTransactionFallback")
    public List<Transaction> findAllByAccountNumber(Integer accountNumber) throws Exception{
        // Obtener trransacciones de BD
        List<Transaction> transactions = transactionRepository.findAllByAccountNumber(accountNumber);

        if(transactions.isEmpty()){
            throw new TransactionsNotFoundException("no se encontraron transacciones para el numero de cuenta: ", accountNumber);
        }
        // si hay transacciones continuar con el agregar logo
        transactions.forEach(transaction -> 
             merchantDetailsRepository
                .findMerchantLogo(transaction.getMerchantName())
                .ifPresent(transaction::setMerchantLogo)
            
        );
        return transactions;
    }


    // fallback invocado por el disyuntor si hay error
    private List<Transaction> handleTransactionFallback(Integer accountNumber, Throwable throwable) throws Exception {
        log.error("Error en la obtencion de transacciones de la API, se pasa a pollByAccountNumer...");

        return pollByAccountNumber(accountNumber);
    }



    //Metodo que comporara las Transacciones recibidas vs las almacenas en BD
    public List<Transaction> pollByAccountNumber(Integer accountNumber) throws Exception {

        // solicitar las transacciones a la API
        List<Transaction> transactionsFromApi = transactionApiClient.findAllByAccountNumber(accountNumber);

        // solicitar las trransacciones a las BD
        List<Transaction> transactionsInDb = transactionRepository.findAllByAccountNumber(accountNumber);
        
        //filtrar nuevas transacciones
        List<Transaction> newTransactions = transactionsFromApi.stream()
            .filter(transaction -> !transactionsInDb.contains(transaction))
            .collect(Collectors.toList());

        if (!newTransactions.isEmpty()) {
            log.info("Se encontraron nuevas Transactions. se Almacenan en base de datos...");
            transactionRepository.saveAll(newTransactions);
        }

        return transactionRepository.findAllByAccountNumber(accountNumber);
    }

       // Método para realizar el sondeo de todas las cuentas
    @Scheduled(fixedRate = 300000) // 300000 ms = 5 minutos
    public void pollTransactionsForAllAccounts() {
        try {
            // Obtener todos los números de cuenta almacenados en la base de datos
            List<Integer> accountNumbers = transactionRepository.findAllAccountNumbers();

            // Realizar el sondeo para cada número de cuenta
            for (Integer accountNumber : accountNumbers) {
                log.info("Iniciando el sondeo para el número de cuenta: " + accountNumber);
                pollByAccountNumber(accountNumber);
            }
        } catch (Exception e) {
            log.error("Error al realizar el sondeo de transacciones: ", e);
        }
    }


    public static class TransactionsNotFoundException extends RuntimeException {
        public TransactionsNotFoundException(String message, Integer accountNumber) {
            super(message + "pa el nuemero de cuenta" + accountNumber);
        }
    }

    // private List<Transaction> findAllByAccountNumber(Integer accountNumber, Throwable throwable){
    //     log.info("Obtener Transacciones desde la BD");
    //     return transactionRepository.findAllByAccountNumber(accountNumber);
    // }

}
