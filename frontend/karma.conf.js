// Karma configuration tuned for deterministic CI execution and coverage gating.
const fs = require('fs');
const path = require('path');

// Resolve a Chromium-compatible browser binary across local dev and CI machines.
function resolveChromeBinary() {
  if (process.env.CHROME_BIN && fs.existsSync(process.env.CHROME_BIN)) {
    return process.env.CHROME_BIN;
  }
  const candidates = [
    'C:/Program Files/Google/Chrome/Application/chrome.exe',
    'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
    'C:/Program Files/Microsoft/Edge/Application/msedge.exe',
    'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  ];
  const edgeCore = 'C:/Program Files (x86)/Microsoft/EdgeCore';
  if (fs.existsSync(edgeCore)) {
    const versions = fs.readdirSync(edgeCore).sort().reverse();
    for (const v of versions) {
      const exe = path.join(edgeCore, v, 'msedge.exe');
      if (fs.existsSync(exe)) {
        candidates.unshift(exe);
        break;
      }
    }
  }
  return candidates.find((p) => fs.existsSync(p));
}

const browserBinary = resolveChromeBinary();
if (browserBinary) {
  process.env.CHROME_BIN = browserBinary;
}

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
    ],
    client: {
      jasmine: {
        random: false,
        timeoutInterval: 10000,
      },
      clearContext: false,
    },
    jasmineHtmlReporter: { suppressAll: true },
    coverageReporter: {
      dir: require('path').join(__dirname, './coverage/frontend'),
      subdir: '.',
      reporters: [
        { type: 'html' },
        { type: 'text-summary' },
        { type: 'lcovonly', file: 'lcov.info' },
        { type: 'cobertura', file: 'cobertura.xml' },
      ],
      check: {
        global: {
          statements: 85,
          branches: 85,
          functions: 85,
          lines: 85,
        },
      },
    },
    reporters: ['progress', 'kjhtml'],
    port: 9876,
    colors: true,
    logLevel: config.LOG_INFO,
    autoWatch: true,
    browsers: ['ChromeHeadlessCI'],
    customLaunchers: {
      ChromeHeadlessCI: {
        base: 'ChromeHeadless',
        flags: ['--no-sandbox', '--disable-gpu', '--disable-dev-shm-usage'],
      },
    },
    singleRun: false,
    restartOnFileChange: true,
  });
};
