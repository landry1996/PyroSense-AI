import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BuildingListComponent } from './building-list.component';
import { ApiService } from '../../core/services/api.service';
import { provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

describe('BuildingListComponent', () => {
  let component: BuildingListComponent;
  let fixture: ComponentFixture<BuildingListComponent>;

  const mockBuildings = [
    { id: 'b1', name: 'Batiment Alpha', address: '1 rue de Paris', status: 'OK', riskScore: 25, totalDevices: 10, activeDevices: 9, highestAlertSeverity: null, lastAlertAt: null },
    { id: 'b2', name: 'Batiment Beta', address: '2 avenue Lyon', status: 'AT_RISK', riskScore: 72, totalDevices: 8, activeDevices: 6, highestAlertSeverity: 'CRITICAL', lastAlertAt: '2026-05-27' },
    { id: 'b3', name: 'Batiment Gamma', address: '3 bd Marseille', status: 'CRITICAL', riskScore: 88, totalDevices: 5, activeDevices: 3, highestAlertSeverity: 'CRITICAL', lastAlertAt: '2026-05-28' },
  ];

  const mockApi = {
    getBuildings: jasmine.createSpy('getBuildings').and.returnValue(of(mockBuildings)),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BuildingListComponent, NoopAnimationsModule],
      providers: [
        { provide: ApiService, useValue: mockApi },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BuildingListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display building cards when loaded', () => {
    fixture.detectChanges();
    const cards = fixture.nativeElement.querySelectorAll('.building-card');
    expect(cards.length).toBe(3);
  });

  it('should have aria-labels on building cards', () => {
    fixture.detectChanges();
    const cards = fixture.nativeElement.querySelectorAll('.building-card[aria-label]');
    expect(cards.length).toBe(3);
    expect(cards[0].getAttribute('aria-label')).toContain('Batiment Alpha');
  });

  it('should have aria-label on search input', () => {
    const input = fixture.nativeElement.querySelector('input[aria-label="Rechercher un batiment"]');
    expect(input).toBeTruthy();
  });

  it('should have role=main on container', () => {
    const container = fixture.nativeElement.querySelector('[role="main"]');
    expect(container).toBeTruthy();
  });

  it('should filter buildings by search term', () => {
    component.searchTerm.set('Alpha');
    fixture.detectChanges();
    const cards = fixture.nativeElement.querySelectorAll('.building-card');
    expect(cards.length).toBe(1);
  });

  it('should show empty state when search yields no results', () => {
    component.searchTerm.set('NONEXISTENT');
    fixture.detectChanges();
    const emptyState = fixture.nativeElement.querySelector('.empty-state');
    expect(emptyState).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('NONEXISTENT');
  });

  it('should sort by risk score', () => {
    component.sortBy.set('risk');
    fixture.detectChanges();
    const filtered = component.filteredBuildings();
    expect(filtered[0].name).toBe('Batiment Gamma');
    expect(filtered[2].name).toBe('Batiment Alpha');
  });

  it('should sort by device count', () => {
    component.sortBy.set('devices');
    fixture.detectChanges();
    const filtered = component.filteredBuildings();
    expect(filtered[0].name).toBe('Batiment Alpha');
  });

  it('should return correct status labels', () => {
    expect(component.getStatusLabel('OK')).toBe('OK');
    expect(component.getStatusLabel('AT_RISK')).toBe('A risque');
    expect(component.getStatusLabel('CRITICAL')).toBe('Critique');
  });

  it('should show loading spinner initially', () => {
    component.loading.set(true);
    fixture.detectChanges();
    const spinner = fixture.nativeElement.querySelector('mat-spinner');
    expect(spinner).toBeTruthy();
  });

  it('should clean up on destroy', () => {
    spyOn(component['destroy$'], 'next');
    component.ngOnDestroy();
    expect(component['destroy$'].next).toHaveBeenCalled();
  });
});
