import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SeverityBadgeComponent } from './severity-badge.component';

describe('SeverityBadgeComponent', () => {
  let component: SeverityBadgeComponent;
  let fixture: ComponentFixture<SeverityBadgeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SeverityBadgeComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SeverityBadgeComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display severity text', () => {
    component.severity = 'CRITICAL';
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.badge').textContent.trim()).toBe('CRITICAL');
  });

  it('should apply severity class', () => {
    component.severity = 'WARNING';
    fixture.detectChanges();

    const badge = fixture.nativeElement.querySelector('.badge');
    expect(badge.classList).toContain('warning');
  });

  it('should default to INFO', () => {
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.badge');
    expect(badge.classList).toContain('info');
  });
});
