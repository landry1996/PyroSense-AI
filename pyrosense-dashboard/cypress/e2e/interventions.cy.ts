describe('Interventions', () => {
  beforeEach(() => {
    cy.login();
    cy.mockApi();
    cy.visit('/interventions');
  });

  it('displays interventions list', () => {
    cy.contains('Interventions');
    cy.contains('Remplacement connexion');
  });

  it('navigates to intervention detail', () => {
    cy.contains('Remplacement connexion').click();
    cy.url().should('match', /\/interventions\/.+/);
  });

  it('shows intervention detail with diagnostic', () => {
    cy.contains('Remplacement connexion').click();
    cy.contains('Diagnostic terrain');
    cy.contains('Connexion oxydee');
  });
});
