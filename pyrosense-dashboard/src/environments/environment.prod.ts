export const environment = {
  production: true,
  apiUrl: '/api/v1',
  keycloak: {
    url: '${KEYCLOAK_URL}',
    realm: 'pyrosense',
    clientId: 'pyrosense-dashboard',
  },
  pollingInterval: 30000,
};
