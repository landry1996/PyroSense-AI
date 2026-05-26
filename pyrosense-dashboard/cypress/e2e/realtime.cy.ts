describe('Real-time WebSocket', () => {
  beforeEach(() => {
    cy.login();
    cy.mockApi();
  });

  it('should display connection indicator in toolbar', () => {
    cy.visit('/dashboard');
    cy.get('.connection-indicator').should('exist');
    cy.get('.connection-indicator mat-icon').should('exist');
  });

  it('should show notification badge in toolbar', () => {
    cy.visit('/dashboard');
    cy.get('[aria-label="Notifications non lues"]').should('exist');
  });

  it('should navigate to alerts page from toolbar', () => {
    cy.visit('/dashboard');
    cy.get('a[routerLink="/alerts"]').first().click();
    cy.url().should('include', '/alerts');
  });

  it('should display wifi icon for connection status', () => {
    cy.visit('/dashboard');
    cy.get('.connection-indicator mat-icon').invoke('text').then(text => {
      expect(['wifi', 'wifi_off']).to.include(text.trim());
    });
  });
});
