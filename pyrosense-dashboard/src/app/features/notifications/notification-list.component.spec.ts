import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NotificationListComponent } from './notification-list.component';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { HttpClient } from '@angular/common/http';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

describe('NotificationListComponent', () => {
  let component: NotificationListComponent;
  let fixture: ComponentFixture<NotificationListComponent>;

  const mockNotifications = [
    { id: '1', severity: 'CRITICAL', subject: 'Alert haute temperature', channel: 'EMAIL', status: 'SENT', createdAt: '2026-05-28T10:00:00Z' },
    { id: '2', severity: 'WARNING', subject: 'Capteur offline', channel: 'PUSH', status: 'PENDING', createdAt: '2026-05-28T09:00:00Z' },
  ];

  const mockStatistics = { sent: 10, pending: 3, retrying: 1, failed: 2 };

  const mockApi = {
    getNotificationsByRecipient: jasmine.createSpy('getNotificationsByRecipient').and.returnValue(of(mockNotifications)),
    getNotificationStatistics: jasmine.createSpy('getNotificationStatistics').and.returnValue(of(mockStatistics)),
  };

  const mockAuth = {
    getUserId: () => 'user-123',
    currentUser: () => ({ tenantId: 'tenant-1' }),
  };

  const mockHttp = {
    get: jasmine.createSpy('get').and.returnValue(of({ userId: 'user-123', consentEmail: true, consentSms: false, consentPush: true })),
    put: jasmine.createSpy('put').and.returnValue(of({})),
  };

  const mockSnackBar = {
    open: jasmine.createSpy('open'),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotificationListComponent, NoopAnimationsModule],
      providers: [
        { provide: ApiService, useValue: mockApi },
        { provide: AuthService, useValue: mockAuth },
        { provide: HttpClient, useValue: mockHttp },
        { provide: MatSnackBar, useValue: mockSnackBar },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NotificationListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show skeleton when loading', () => {
    component.loading.set(true);
    fixture.detectChanges();
    const skeletons = fixture.nativeElement.querySelectorAll('app-skeleton');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('should display notifications table after loading', () => {
    fixture.detectChanges();
    const table = fixture.nativeElement.querySelector('.notifications-table');
    expect(table).toBeTruthy();
  });

  it('should display statistics with aria-labels', () => {
    fixture.detectChanges();
    const labeled = fixture.nativeElement.querySelectorAll('.stat-card[aria-label]');
    expect(labeled.length).toBe(4);
  });

  it('should show empty state when no notifications', () => {
    component.loading.set(false);
    component.notifications.set([]);
    fixture.detectChanges();
    const emptyState = fixture.nativeElement.querySelector('app-empty-state');
    expect(emptyState).toBeTruthy();
  });

  it('should define 3 preference channels', () => {
    expect(component.preferences().consentEmail).toBeDefined();
    expect(component.preferences().consentSms).toBeDefined();
    expect(component.preferences().consentPush).toBeDefined();
  });

  it('should return correct severity icons', () => {
    expect(component.getSeverityIcon('CRITICAL')).toBe('error');
    expect(component.getSeverityIcon('WARNING')).toBe('warning');
    expect(component.getSeverityIcon('INFO')).toBe('info');
  });

  it('should return correct channel labels', () => {
    expect(component.getChannelLabel('EMAIL')).toBe('Email');
    expect(component.getChannelLabel('SMS')).toBe('SMS');
    expect(component.getChannelLabel('PUSH')).toBe('Push');
  });

  it('should return correct status labels', () => {
    expect(component.getStatusLabel('PENDING')).toBe('En attente');
    expect(component.getStatusLabel('SENT')).toBe('Envoyee');
    expect(component.getStatusLabel('FAILED')).toBe('Echouee');
  });

  it('should save preferences', () => {
    component.savePreferences();
    expect(mockHttp.put).toHaveBeenCalled();
  });

  it('should clean up on destroy', () => {
    spyOn(component['destroy$'], 'next');
    component.ngOnDestroy();
    expect(component['destroy$'].next).toHaveBeenCalled();
  });
});
