describe('Buildings', () => {
  beforeEach(() => {
    cy.login();
    cy.mockApi();
    cy.visit('/buildings');
  });

  it('displays building list', () => {
    cy.contains('Batiments');
    cy.contains('Immeuble Voltaire');
    cy.contains('Residence Les Lilas');
  });

  it('shows risk level indicators', () => {
    cy.contains('AT_RISK').should('exist');
  });

  it('navigates to building detail on click', () => {
    cy.contains('Immeuble Voltaire').click();
    cy.url().should('match', /\/buildings\/.+/);
  });
});
