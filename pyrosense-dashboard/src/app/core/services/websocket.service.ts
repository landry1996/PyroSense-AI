import { Injectable, signal, OnDestroy, NgZone } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { Subject, Observable } from 'rxjs';
import { AuthService } from './auth.service';

export interface WsEvent {
  eventType: string;
  timestamp: string;
  data: any;
}

@Injectable({ providedIn: 'root' })
export class WebSocketService implements OnDestroy {
  private client: Client | null = null;
  private subscriptions: Map<string, { unsubscribe: () => void }> = new Map();
  private eventSubjects: Map<string, Subject<WsEvent>> = new Map();

  readonly connected = signal(false);
  readonly reconnecting = signal(false);

  constructor(private auth: AuthService, private zone: NgZone) {}

  connect(): void {
    if (this.client?.active) return;

    const tenantId = this.auth.currentUser()?.tenantId;
    if (!tenantId) return;

    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${wsProtocol}//${window.location.host}/ws`;

    this.client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        this.zone.run(() => {
          this.connected.set(true);
          this.reconnecting.set(false);
        });
        this.subscribeToTenant(tenantId);
      },
      onDisconnect: () => {
        this.zone.run(() => this.connected.set(false));
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message']);
        this.zone.run(() => this.connected.set(false));
      },
      onWebSocketClose: () => {
        this.zone.run(() => {
          this.connected.set(false);
          this.reconnecting.set(true);
        });
      },
    });

    this.client.activate();
  }

  disconnect(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.subscriptions.clear();
    this.client?.deactivate();
    this.connected.set(false);
  }

  onAlerts(): Observable<WsEvent> {
    return this.getOrCreateSubject('alerts').asObservable();
  }

  onDashboard(): Observable<WsEvent> {
    return this.getOrCreateSubject('dashboard').asObservable();
  }

  onTelemetry(deviceId: string): Observable<WsEvent> {
    const tenantId = this.auth.currentUser()?.tenantId;
    if (!tenantId || !this.client?.active) return new Subject<WsEvent>().asObservable();

    const topic = `/topic/tenant.${tenantId}.telemetry.${deviceId}`;
    if (!this.subscriptions.has(topic)) {
      const sub = this.client.subscribe(topic, (msg: IMessage) => {
        this.handleMessage('telemetry.' + deviceId, msg);
      });
      this.subscriptions.set(topic, sub);
    }
    return this.getOrCreateSubject('telemetry.' + deviceId).asObservable();
  }

  private subscribeToTenant(tenantId: string): void {
    if (!this.client) return;

    const alertTopic = `/topic/tenant.${tenantId}.alerts`;
    const dashTopic = `/topic/tenant.${tenantId}.dashboard`;

    if (!this.subscriptions.has(alertTopic)) {
      const sub = this.client.subscribe(alertTopic, (msg: IMessage) => {
        this.handleMessage('alerts', msg);
      });
      this.subscriptions.set(alertTopic, sub);
    }

    if (!this.subscriptions.has(dashTopic)) {
      const sub = this.client.subscribe(dashTopic, (msg: IMessage) => {
        this.handleMessage('dashboard', msg);
      });
      this.subscriptions.set(dashTopic, sub);
    }
  }

  private handleMessage(channel: string, message: IMessage): void {
    try {
      const event: WsEvent = JSON.parse(message.body);
      this.zone.run(() => {
        this.getOrCreateSubject(channel).next(event);
      });
    } catch (e) {
      console.warn('Failed to parse WebSocket message:', e);
    }
  }

  private getOrCreateSubject(key: string): Subject<WsEvent> {
    if (!this.eventSubjects.has(key)) {
      this.eventSubjects.set(key, new Subject<WsEvent>());
    }
    return this.eventSubjects.get(key)!;
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.eventSubjects.forEach(s => s.complete());
  }
}
