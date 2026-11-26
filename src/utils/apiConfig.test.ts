import test from 'node:test';
import assert from 'node:assert/strict';
import { resolveApiUrl } from './apiConfig';

test('resolveApiUrl uses relative path on web when no override is configured', () => {
  assert.equal(
    resolveApiUrl('/api/mods/generate', {
      isNative: false,
      configuredBaseUrl: '',
      envBaseUrl: '',
    }),
    '/api/mods/generate'
  );
});

test('resolveApiUrl uses default cloud backend URL on native Android Capacitor', () => {
  assert.equal(
    resolveApiUrl('api/mods/generate', {
      isNative: true,
      configuredBaseUrl: '',
      envBaseUrl: '',
      defaultNativeBaseUrl: 'https://example.run.app/',
    }),
    'https://example.run.app/api/mods/generate'
  );
});

test('resolveApiUrl prioritizes user configuredBaseUrl over env and default', () => {
  assert.equal(
    resolveApiUrl('/api/mods/generate', {
      isNative: true,
      configuredBaseUrl: 'https://custom.example.com///',
      envBaseUrl: 'https://env.example.com',
    }),
    'https://custom.example.com/api/mods/generate'
  );
});
