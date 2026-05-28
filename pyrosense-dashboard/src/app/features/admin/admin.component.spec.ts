import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminComponent } from './admin.component';
import { AuthService } from '../../core/services/auth.service';
import { HttpClient } from '@angular/common/http';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

describe('AdminComponent', () => {
  let component: AdminComponent;
  let fixture: ComponentFixture<AdminComponent>;

  const mockUsers = [
    { id: '1', email: 'admin@test.com', fullName: 'Admin User', status: 'ACTIVE', createdAt: '2026-01-01', lastLoginAt: '2026-05-28' },
    { id: '2', email: 'user@test.com', fullName: 'Regular User', status: 'LOCKED', createdAt: '2026-02-01', lastLoginAt: null },
  ];

  const mockAuditEntries = [
    { id: '1', action: 'LOGIN', resourceType: 'USER', resourceId: 'user-1', userId: 'user-1', ipAddress: '192.168.1.1', details: null, timestamp: '2026-05-28T10:00:00Z' },
    { id: '2', action: 'UPDATE', resourceType: 'DEVICE', resourceId: 'dev-1', userId: 'user-1', ipAddress: null, details: null, timestamp: '2026-05-28T09:30:00Z' },
  ];

  const mockAuth = {
    currentUser: () => ({ tenantId: 'tenant-1' }),
  };

  const mockHttp = {
    get: jasmine.createSpy('get').and.callFake((url: string) => {
      if (url.includes('users')) return of(mockUsers);
      if (url.includes('audit-log')) return of(mockAuditEntries);
      return of([]);
    }),
  };

  const mockSnackBar = { open: jasmine.createSpy('open') };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminComponent, NoopAnimationsModule],
      providers: [
        { provide: AuthService, useValue: mockAuth },
        { provide: HttpClient, useValue: mockHttp },
        { provide: MatSnackBar, useValue: mockSnackBar },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show skeleton when users loading', () => {
    component.usersLoading.set(true);
    fixture.detectChanges();
    const skeleton = fixture.nativeElement.querySelector('app-skeleton');
    expect(skeleton).toBeTruthy();
  });

  it('should display users table after loading', () => {
    fixture.detectChanges();
    const table = fixture.nativeElement.querySelector('.users-table');
    expect(table).toBeTruthy();
  });

  it('should display user names', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Admin User');
  });

  it('should show empty state when no users', () => {
    component.usersLoading.set(false);
    component.users.set([]);
    fixture.detectChanges();
    const emptyState = fixture.nativeElement.querySelector('app-empty-state');
    expect(emptyState).toBeTruthy();
  });

  it('should load audit data on init', () => {
    expect(mockHttp.get).toHaveBeenCalledWith(jasmine.stringContaining('audit-log'));
  });

  it('should store audit entries in signal', () => {
    expect(component.auditEntries().length).toBe(2);
    expect(component.auditEntries()[0].action).toBe('LOGIN');
  });

  it('should set auditLoading to false after load', () => {
    expect(component.auditLoading()).toBeFalse();
  });

  it('should have role=main on container', () => {
    const container = fixture.nativeElement.querySelector('[role="main"]');
    expect(container).toBeTruthy();
  });

  it('should have page title with id', () => {
    const title = fixture.nativeElement.querySelector('#admin-title');
    expect(title).toBeTruthy();
    expect(title.textContent).toContain('Administration');
  });

  it('should clean up on destroy', () => {
    spyOn(component['destroy$'], 'next');
    component.ngOnDestroy();
    expect(component['destroy$'].next).toHaveBeenCalled();
  });
});
