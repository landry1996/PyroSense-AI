export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1',
  keycloak: {
    url: 'http://localhost:8180',
    realm: 'pyrosense',
    clientId: 'pyrosense-dashboard',
  },
  pollingInterval: 30000,
};
