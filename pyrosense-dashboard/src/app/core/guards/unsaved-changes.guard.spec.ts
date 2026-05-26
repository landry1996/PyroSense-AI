import { unsavedChangesGuard, HasUnsavedChanges } from './unsaved-changes.guard';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

describe('unsavedChangesGuard', () => {
  const mockRoute = {} as ActivatedRouteSnapshot;
  const mockCurrentState = {} as RouterStateSnapshot;
  const mockNextState = {} as RouterStateSnapshot;

  it('should return true when component has no unsaved changes', () => {
    const component: HasUnsavedChanges = { hasUnsavedChanges: () => false };
    const result = unsavedChangesGuard(component, mockRoute, mockCurrentState, mockNextState);
    expect(result).toBeTrue();
  });

  it('should call confirm when component has unsaved changes', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    const component: HasUnsavedChanges = { hasUnsavedChanges: () => true };
    const result = unsavedChangesGuard(component, mockRoute, mockCurrentState, mockNextState);
    expect(window.confirm).toHaveBeenCalled();
    expect(result).toBeTrue();
  });

  it('should return false when user cancels confirm', () => {
    spyOn(window, 'confirm').and.returnValue(false);
    const component: HasUnsavedChanges = { hasUnsavedChanges: () => true };
    const result = unsavedChangesGuard(component, mockRoute, mockCurrentState, mockNextState);
    expect(result).toBeFalse();
  });

  it('should return true when component does not implement interface', () => {
    const component = {} as HasUnsavedChanges;
    const result = unsavedChangesGuard(component, mockRoute, mockCurrentState, mockNextState);
    expect(result).toBeTrue();
  });
});
