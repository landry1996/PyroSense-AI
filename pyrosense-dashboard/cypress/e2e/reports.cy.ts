describe('Reports', () => {
  beforeEach(() => {
    cy.login();
    cy.mockApi();
    cy.visit('/reports');
  });

  it('displays reports list', () => {
    cy.contains('Rapports');
    cy.contains('MH-202605-00001');
  });

  it('shows report types', () => {
    cy.contains('MONTHLY_REPORT').should('exist');
    cy.contains('COMPLIANCE_CERTIFICATE').should('exist');
  });
});
