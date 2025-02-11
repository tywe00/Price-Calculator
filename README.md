# Price Calculator Service

## Overview

The Price Calculator is a backend service designed to calculate the total price and price breakdown for delivery orders. This service integrates with the Home Assignment API to fetch venue-related data and provides a comprehensive pricing mechanism for delivery orders.

## Key Features

- **Single Endpoint**: GET `/api/v1/delivery-order-price`
- **Flexible Integration**: Compatible with backend services, mobile apps, and third-party integrations
- **Comprehensive Pricing Calculation**:
  - Calculates total order price
  - Computes small order surcharges
  - Determines delivery fees based on distance

## Request Parameters

The service requires the following query parameters:

- `venue_slug`: Unique venue identifier
- `cart_value`: Total value of items in the shopping cart
- `user_lat`: User's latitude
- `user_lon`: User's longitude

## Response Structure

```json
{
  "total_price": 1190,
  "small_order_surcharge": 0,
  "cart_value": 1000,
  "delivery": {
    "fee": 190,
    "distance": 177
  }
}
```

## Pricing Calculation Logic

- **Small Order Surcharge**: Calculated as the difference between order minimum and cart value
- **Delivery Distance**: Straight-line distance between user and venue
- **Delivery Fee**: Computed using a base price, distance-based multipliers, and additional constants

## Technical Details

- Supports multiple international venues
- Handles pricing in local currency denominations
- Provides clear error handling for invalid delivery scenarios

## Integration Notes

- Seamlessly integrates with Home Assignment API (provided by wolt)
- Supports venues across different countries
- Flexible and scalable design for various use cases

## How to run?
To run the app:

1) run `./gradlew build`
2) run `./gradlew run`

