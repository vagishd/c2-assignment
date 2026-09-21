package com.support.ticketai.controller;

import com.support.ticketai.dto.AskRequest;
import com.support.ticketai.dto.AskResponse;
import com.support.ticketai.rag.AskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "Assistant", description = "Grounded question answering over ticket history")
public class AiController {

    private final AskService askService;

    @PostMapping("/ask")
    @Operation(summary = "Ask a natural-language question grounded strictly in ticket data. "
            + "The answer cites the ticket ID(s) used, and returns an explicit no-match response "
            + "when no relevant tickets are found.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grounded answer or honest no-match"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return askService.ask(request.question());
    }
}
