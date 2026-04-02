import { mkdirSync, rmSync, readdirSync, statSync, writeFileSync, copyFileSync, existsSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { join, dirname, relative } from 'node:path';

const outDir = 'out';
const classesDir = `${outDir}/classes`;
const distDir = `${outDir}/dist`;
const jarPath = `${distDir}/Audiolayer.jar`;

rmSync(outDir, { recursive: true, force: true });
mkdirSync(classesDir, { recursive: true });
mkdirSync(distDir, { recursive: true });

const mainSources = listJava('src/main/java').filter((file) => !file.endsWith('AudiolayerMod.java') && !file.endsWith('AudiolayerCommands.java'));
execFileSync('javac', ['-d', classesDir, ...mainSources], { stdio: 'inherit' });

const manifestPath = `${outDir}/MANIFEST.MF`;
writeFileSync(manifestPath, 'Manifest-Version: 1.0\nMain-Class: com.audiolayer.testsupport.TestRunner\n');

const jarArgs = ['--create', '--file', jarPath, '--manifest', manifestPath, '-C', classesDir, '.'];
execFileSync('jar', jarArgs, { stdio: 'inherit' });

console.log(`Built ${jarPath}`);

function listJava(root) {
  const result = [];
  function walk(dir) {
    for (const entry of readdirSync(dir)) {
      const full = join(dir, entry);
      const stat = statSync(full);
      if (stat.isDirectory()) {
        walk(full);
      } else if (full.endsWith('.java')) {
        result.push(full);
      }
    }
  }
  walk(root);
  return result;
}
