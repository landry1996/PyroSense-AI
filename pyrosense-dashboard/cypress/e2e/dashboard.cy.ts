describe('Dashboard', () => {
  beforeEach(() => {
    cy.login();
    cy.mockApi();
    cy.visit('/dashboard');
  });

  it('displays 6 stat cards with data', () => {
    cy.get('.stat-card').should('have.length', 6);
    cy.contains('Batiments');
    cy.contains('Capteurs actifs');
    cy.contains('Alertes critiques');
    cy.contains('Interventions en retard');
  });

  it('shows risk gauge and chart', () => {
    cy.get('app-risk-gauge').should('exist');
    cy.get('canvas').should('exist');
  });

  it('navigates to alerts from critical card', () => {
    cy.get('.stat-card.critical').click();
    cy.url().should('include', '/alerts');
  });

  it('navigates to buildings from buildings card', () => {
    cy.get('.stat-card.clickable').first().click();
    cy.url().should('include', '/buildings');
  });
});
