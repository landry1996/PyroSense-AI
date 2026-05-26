describe('Alerts', () => {
  beforeEach(() => {
    cy.login();
    cy.mockApi();
    cy.visit('/alerts');
  });

  it('displays alert statistics cards', () => {
    cy.get('.stat-card').should('have.length.gte', 4);
    cy.contains('Critiques ouvertes');
  });

  it('displays alerts table', () => {
    cy.get('table').should('exist');
    cy.contains('Arc electrique detecte');
  });

  it('shows severity badges', () => {
    cy.get('.severity-badge').should('have.length.gte', 1);
  });

  it('navigates to alert detail', () => {
    cy.contains('Arc electrique detecte').click();
    cy.url().should('match', /\/alerts\/.+/);
    cy.contains('MICRO_ARC_DETECTED');
  });

  it('shows alert detail with comments', () => {
    cy.contains('Arc electrique detecte').click();
    cy.contains('Verification en cours');
  });
});
