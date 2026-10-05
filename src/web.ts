import { WebPlugin } from '@capacitor/core';

import type { LocationPluginPlugin, VerifiedPosition } from './definitions';

export class LocationPluginWeb extends WebPlugin implements LocationPluginPlugin {
  checkMockLastKnown(): Promise<{ isMock: boolean; available: boolean }> {
    throw new Error('Method not implemented.');
  }
  async initialize(): Promise<{ isEnabled: boolean }> {
    console.log('Checking location enabled status...');
    // For the web, you can default to false, or implement actual checks if needed.
    return { isEnabled: false }; // Assume false for web as location services vary
  }

  async isEnabled(): Promise<{ isEnabled: boolean }> {
    return { isEnabled: false };
  }

  async checkMock(): Promise<{ isMock: boolean; available: boolean }> {
    return { isMock: false, available: false };
  }

  async getVerifiedPosition(): Promise<VerifiedPosition> {
    return { isMock: false, available: false };
  }

  async openDeveloperSettings(): Promise<void> {
    return;
  }
}
