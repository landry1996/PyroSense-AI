# PyroSense Notification Service

## Responsibility
Multi-channel notification delivery for alerts and system events.

## Bounded Context
Notification & Delivery

## Key Features
- Email notifications (Spring Mail)
- SMS notifications (Twilio integration)
- Push notifications
- Webhook delivery
- Delivery status tracking (PENDING, SENT, FAILED)

## Events Consumed
- `alerting.alert.created`
- `alerting.alert.escalated`

## Port
8087

## Dependencies
- PostgreSQL (notification log)
- Kafka (event consumption)
- SMTP server (email)
