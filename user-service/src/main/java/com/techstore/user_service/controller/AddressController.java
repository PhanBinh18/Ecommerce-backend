package com.techstore.user_service.controller;

import com.techstore.user_service.dto.AddressRequest;
import com.techstore.user_service.dto.AddressResponse;
import com.techstore.user_service.dto.ApiResponse;
import com.techstore.user_service.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/addresses")
public class AddressController {

    private final UserService userService;

    public AddressController(UserService userService) {
        this.userService = userService;
    }

    // GET /api/v1/users/addresses
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses() {
        List<AddressResponse> list = userService.getAddressesForCurrentUser();
        ApiResponse<List<AddressResponse>> resp = ApiResponse.<List<AddressResponse>>builder()
                .status("SUCCESS")
                .message("OK")
                .data(list)
                .build();
        return ResponseEntity.ok(resp);
    }

    // POST /api/v1/users/addresses
    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(@RequestBody AddressRequest request) {
        AddressResponse created = userService.createAddress(request);
        ApiResponse<AddressResponse> resp = ApiResponse.<AddressResponse>builder()
                .status("SUCCESS")
                .message("Address created")
                .data(created)
                .build();
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    // PUT /api/v1/users/addresses/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(@PathVariable Long id,
                                                                      @RequestBody AddressRequest request) {
        AddressResponse updated = userService.updateAddress(id, request);
        ApiResponse<AddressResponse> resp = ApiResponse.<AddressResponse>builder()
                .status("SUCESS")
                .message("Address updated")
                .data(updated)
                .build();
        return ResponseEntity.ok(resp);
    }
}