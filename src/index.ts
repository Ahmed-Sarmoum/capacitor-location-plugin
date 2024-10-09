import { registerPlugin } from '@capacitor/core';

import type { LocationPluginPlugin } from './definitions';

const LocationPlugin = registerPlugin<LocationPluginPlugin>('LocationPlugin', {
  web: () => import('./web').then((m) => new m.LocationPluginWeb()),
});

export * from './definitions';
export { LocationPlugin };
