import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EmptyStateComponent } from './empty-state.component';

describe('EmptyStateComponent', () => {
  let component: EmptyStateComponent;
  let fixture: ComponentFixture<EmptyStateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmptyStateComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(EmptyStateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have role=status for accessibility', () => {
    const el = fixture.nativeElement.querySelector('[role="status"]');
    expect(el).toBeTruthy();
  });

  it('should display default title', () => {
    expect(fixture.nativeElement.textContent).toContain('Aucune donnee');
  });

  it('should display custom title and message', () => {
    component.title = 'Aucune alerte';
    component.message = 'Tout va bien';
    component.icon = 'check_circle';
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Aucune alerte');
    expect(fixture.nativeElement.textContent).toContain('Tout va bien');
  });

  it('should render the icon', () => {
    component.icon = 'inbox';
    fixture.detectChanges();
    const icon = fixture.nativeElement.querySelector('mat-icon');
    expect(icon.textContent.trim()).toBe('inbox');
  });
});
