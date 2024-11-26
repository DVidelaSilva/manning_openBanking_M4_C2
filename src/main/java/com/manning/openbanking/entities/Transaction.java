package com.manning.openbanking.entities;

import java.util.Date;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String type;
    private Date date;
    private Integer accountNumber;
    private String currency;
    private Double amount;
    private String merchantName;
    private String merchantLogo;


    // //Metodo para comparar transacciones
    // @Override
    // public boolean equals(Object object) {
    //     if (this == object) return true;
    //     if (object == null || getClass() != object.getClass())
    //     return false;

    //     Transaction that = (Transaction) object;
    //     return Objects.equals(id, that.id);
        
    // }


    // @Override
    // public int hashCode() {
    //     return Objects.hash(id);
    // }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(date, that.date) &&
                Objects.equals(accountNumber, that.accountNumber)
                && Objects.equals(currency, that.currency)
                && Objects.equals(amount, that.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, date, accountNumber, currency, amount);
    }


}
