package com.dws.challenge.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotNull
    @NotEmpty(message = "accountFromId missing")
    private String accountFromId;

    @NotNull
    @NotEmpty(message = "accountToId missing")
    private String accountToId;

    @NotNull
    private BigDecimal amount;
}
