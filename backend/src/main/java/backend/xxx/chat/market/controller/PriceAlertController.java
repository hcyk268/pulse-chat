package backend.xxx.chat.market.controller;

import java.util.List;

import backend.xxx.chat.common.dto.ResponseData;
import backend.xxx.chat.common.security.CurrentUserProvider;
import backend.xxx.chat.market.dto.CreatePriceAlertRequest;
import backend.xxx.chat.market.dto.PriceAlertResponse;
import backend.xxx.chat.market.dto.UpdatePriceAlertRequest;
import backend.xxx.chat.market.service.PriceAlertService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/market/price-alerts")
@RequiredArgsConstructor
@Validated
public class PriceAlertController {

    private final CurrentUserProvider currentUserProvider;
    private final PriceAlertService priceAlertService;

    @PostMapping
    public ResponseEntity<ResponseData<PriceAlertResponse>> createPriceAlert(@Valid @RequestBody CreatePriceAlertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseData<>(
                true,
                "market.price-alert.create.success",
                priceAlertService.createPriceAlert(currentUserProvider.getCurrentUsername(), request)
        ));
    }

    @GetMapping
    public ResponseData<List<PriceAlertResponse>> getPriceAlerts() {
        return new ResponseData<>(
                true,
                "market.price-alert.list.success",
                priceAlertService.getPriceAlerts(currentUserProvider.getCurrentUsername())
        );
    }

    @GetMapping("/{id}")
    public ResponseData<PriceAlertResponse> getPriceAlert(@Positive @PathVariable Long id) {
        return new ResponseData<>(
                true,
                "market.price-alert.detail.success",
                priceAlertService.getPriceAlert(currentUserProvider.getCurrentUsername(), id)
        );
    }

    @PatchMapping("/{id}")
    public ResponseData<PriceAlertResponse> updatePriceAlert(
            @Positive @PathVariable Long id,
            @Valid @RequestBody UpdatePriceAlertRequest request
    ) {
        return new ResponseData<>(
                true,
                "market.price-alert.update.success",
                priceAlertService.updatePriceAlert(currentUserProvider.getCurrentUsername(), id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseData<Void> deletePriceAlert(@Positive @PathVariable Long id) {
        priceAlertService.deletePriceAlert(currentUserProvider.getCurrentUsername(), id);
        return new ResponseData<>(true, "market.price-alert.delete.success");
    }
}
