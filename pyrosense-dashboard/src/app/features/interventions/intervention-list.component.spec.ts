import { ComponentFixture, TestBed } from '@angular/core/testing';
import { InterventionListComponent } from './intervention-list.component';
import { ApiService } from '../../core/services/api.service';
import { provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, EMPTY } from 'rxjs';

describe('InterventionListComponent', () => {
  let component: InterventionListComponent;
  let fixture: ComponentFixture<InterventionListComponent>;

  const mockKanban = {
    created: [{ id: '1', priority: 'HIGH', type: 'CORRECTIVE', description: 'Test intervention', assignedElectricianId: null, scheduledAt: null }],
    planned: [],
    assigned: [],
    inProgress: [{ id: '2', priority: 'URGENT', type: 'EMERGENCY', description: 'Urgent fix', assignedElectricianId: 'user-1', scheduledAt: '2026-05-30' }],
    completed: [],
  };

  const mockStatistics = {
    total: 12,
    inProgress: 3,
    completed: 7,
    falsePositives: 2,
    averageRiskReduction: 35,
  };

  const mockApi = {
    getInterventionKanban: jasmine.createSpy('getInterventionKanban').and.returnValue(of(mockKanban)),
    getInterventionStatistics: jasmine.createSpy('getInterventionStatistics').and.returnValue(of(mockStatistics)),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InterventionListComponent, NoopAnimationsModule],
      providers: [
        { provide: ApiService, useValue: mockApi },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(InterventionListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show skeleton loader when loading', () => {
    component.loading.set(true);
    component.kanban.set(null);
    fixture.detectChanges();
    const skeletons = fixture.nativeElement.querySelectorAll('app-skeleton');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('should display kanban columns when data loaded', () => {
    fixture.detectChanges();
    const columns = fixture.nativeElement.querySelectorAll('.kanban-column');
    expect(columns.length).toBe(5);
  });

  it('should display statistics cards', () => {
    fixture.detectChanges();
    const statCards = fixture.nativeElement.querySelectorAll('.stat-card');
    expect(statCards.length).toBe(5);
    expect(fixture.nativeElement.textContent).toContain('12');
  });

  it('should have aria-labels on stat cards', () => {
    fixture.detectChanges();
    const labeled = fixture.nativeElement.querySelectorAll('.stat-card[aria-label]');
    expect(labeled.length).toBe(5);
  });

  it('should have role=list on kanban columns', () => {
    fixture.detectChanges();
    const lists = fixture.nativeElement.querySelectorAll('[role="list"]');
    expect(lists.length).toBe(5);
  });

  it('should show empty state when no kanban data', () => {
    component.loading.set(false);
    component.kanban.set(null);
    fixture.detectChanges();
    const emptyState = fixture.nativeElement.querySelector('app-empty-state');
    expect(emptyState).toBeTruthy();
  });

  it('should display priority labels correctly', () => {
    expect(component.getPriorityLabel('URGENT')).toBe('Urgent');
    expect(component.getPriorityLabel('HIGH')).toBe('Haute');
    expect(component.getPriorityLabel('MEDIUM')).toBe('Moyenne');
    expect(component.getPriorityLabel('LOW')).toBe('Basse');
  });

  it('should display type labels correctly', () => {
    expect(component.getTypeLabel('PREVENTIVE')).toBe('Preventive');
    expect(component.getTypeLabel('EMERGENCY')).toBe('Urgence');
  });

  it('should clean up on destroy', () => {
    spyOn(component['destroy$'], 'next');
    component.ngOnDestroy();
    expect(component['destroy$'].next).toHaveBeenCalled();
  });
});
