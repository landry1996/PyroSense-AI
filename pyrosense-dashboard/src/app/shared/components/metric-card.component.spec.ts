import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MetricCardComponent } from './metric-card.component';
import { provideRouter } from '@angular/router';

describe('MetricCardComponent', () => {
  let component: MetricCardComponent;
  let fixture: ComponentFixture<MetricCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MetricCardComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(MetricCardComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display value and label', () => {
    component.value = 42;
    component.label = 'Capteurs actifs';
    component.icon = 'sensors';
    fixture.detectChanges();

    const el = fixture.nativeElement;
    expect(el.querySelector('.metric-value').textContent).toContain('42');
    expect(el.querySelector('.metric-label').textContent).toContain('Capteurs actifs');
  });

  it('should apply variant class', () => {
    component.variant = 'critical';
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector('.metric-card');
    expect(card.classList).toContain('critical');
  });

  it('should be clickable when link provided', () => {
    component.link = '/devices';
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector('.metric-card');
    expect(card.classList).toContain('clickable');
  });
});
