package com.bikeshop.reviews.dto;

import jakarta.validation.constraints.NotNull;

public record ModerateReviewRequest(@NotNull Boolean aprovado) {
}
