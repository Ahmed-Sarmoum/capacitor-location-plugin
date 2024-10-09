import { registerPlugin } from '@capacitor/core';

import type { LocationPlugin } from './definitions';

const LocationPlugin = registerPlugin<LocationPlugin>('LocationPlugin', {
  web: () => import('./web').then((m) => new m.LocationPluginWeb()),
});

export * from './definitions';
export { LocationPlugin };
