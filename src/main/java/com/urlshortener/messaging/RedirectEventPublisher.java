package com.urlshortener.messaging;

public interface RedirectEventPublisher {
    void publish(RedirectEvent event);
}
