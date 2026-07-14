package com.bikeshop.cart;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CartRedisRepository {

    private static final String KEY_PREFIX = "cart:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Duration ttl;

    public CartRedisRepository(RedisTemplate<String, Object> redisTemplate,
                                @Value("${bikeshop.cart.ttl-days:30}") long ttlDays) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofDays(ttlDays);
    }

    public Carrinho findOrCreate(String cartId) {
        Carrinho cart = (Carrinho) redisTemplate.opsForValue().get(KEY_PREFIX + cartId);
        return cart != null ? cart : new Carrinho(cartId);
    }

    public void save(Carrinho cart) {
        redisTemplate.opsForValue().set(KEY_PREFIX + cart.getId(), cart, ttl);
    }

    public void delete(String cartId) {
        redisTemplate.delete(KEY_PREFIX + cartId);
    }
}
