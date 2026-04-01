package com.fooddelivery.orderservice.fallback;

import com.fooddelivery.orderservice.client.RestaurantClient;
import com.fooddelivery.orderservice.exception.ServiceUnavailableException;
import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class RestaurantClientFallback implements FallbackFactory<RestaurantClient> {

    /**
     * Returns an instance of the fallback appropriate for the given cause.
     *
     * @param cause cause of an exception.
     * @return fallback
     */
    @Override
    public RestaurantClient create(Throwable cause) {
        System.err.println("=== FALLBACK TRIGGERED ===");
        System.err.println("Cause: " + cause.getClass().getName());
//        System.err.println("Message: " + cause.getMessage());
        // Unwrap ExecutionException to get the actual FeignException
        Throwable actualCause = cause;
        if (cause instanceof ExecutionException && cause.getCause() != null) {
            actualCause = cause.getCause();
        }

        System.err.println("Actual cause type: " + actualCause.getClass().getName());

        final Throwable finalCause = actualCause;
        return new RestaurantClient() {
            @Override
            public RestaurantClient.RestaurantResponseDTO getRestaurantById(Long id) {
                if (finalCause instanceof FeignException fe) {
                    if (fe.status() >= 400 && fe.status() < 500) {
                        throw new RuntimeException(fe.contentUTF8(), cause);
                    }
                }

                throw new ServiceUnavailableException("Restaurant Service is currently unavailable. Please try again later.");
            }

            @Override
            public RestaurantClient.MenuItemResponseDTO getMenuItemByRestaurant(Long restaurantId, Long menuItemId) {
                if (finalCause instanceof FeignException fe) {
                if (fe.status() >= 400 && fe.status() < 500) {
                throw new RuntimeException(fe.contentUTF8(), cause);}
            }

                throw new ServiceUnavailableException("Menu Service is currently unavailable. Please try again later.");
            }


        };
    }
}