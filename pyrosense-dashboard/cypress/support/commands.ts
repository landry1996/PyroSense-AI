Cypress.Commands.add('login', () => {
  cy.intercept('GET', '**/realms/pyrosense/**', { statusCode: 200, body: {} });
  cy.intercept('POST', '**/token', {
    statusCode: 200,
    body: { access_token: 'mock-jwt-token', token_type: 'bearer', expires_in: 3600 },
  });
  cy.intercept('GET', '**/userinfo', {
    statusCode: 200,
    body: { sub: 'user-001', email: 'admin@pyrosense.io', name: 'Admin Test', tenant_id: 'tenant-001' },
  });
});

Cypress.Commands.add('mockApi', () => {
  cy.intercept('GET', '/api/v1/devices/statistics*', { fixture: 'device-stats.json' }).as('deviceStats');
  cy.intercept('GET', '/api/v1/risk-scoring/tenant/summary*', { fixture: 'risk-summary.json' }).as('riskSummary');
  cy.intercept('GET', '/api/v1/risk-scoring/tenant/history*', { fixture: 'risk-history.json' }).as('riskHistory');
  cy.intercept('GET', '/api/v1/alerts/statistics*', { fixture: 'alert-stats.json' }).as('alertStats');
  cy.intercept('GET', '/api/v1/interventions/overdue*', { fixture: 'overdue-interventions.json' }).as('overdueInterventions');
  cy.intercept('GET', '/api/v1/buildings*', { fixture: 'buildings.json' }).as('buildings');
  cy.intercept('GET', '/api/v1/alerts?*', { fixture: 'alerts.json' }).as('alerts');
  cy.intercept('GET', '/api/v1/alerts/*', { fixture: 'alert-detail.json' }).as('alertDetail');
  cy.intercept('GET', '/api/v1/interventions?*', { fixture: 'interventions.json' }).as('interventions');
  cy.intercept('GET', '/api/v1/interventions/*', { fixture: 'intervention-detail.json' }).as('interventionDetail');
  cy.intercept('GET', '/api/v1/reports*', { fixture: 'reports.json' }).as('reports');
  cy.intercept('GET', '/api/v1/notifications*', { fixture: 'notifications.json' }).as('notifications');
  cy.intercept('GET', '/api/v1/notifications/statistics*', { fixture: 'notification-stats.json' }).as('notifStats');
});

declare global {
  namespace Cypress {
    interface Chainable {
      login(): Chainable<void>;
      mockApi(): Chainable<void>;
    }
  }
}

export {};
