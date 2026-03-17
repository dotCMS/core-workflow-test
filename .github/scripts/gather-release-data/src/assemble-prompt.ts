#!/usr/bin/env node

/**
 * assemble-prompt
 *
 * Reads a prompt template and a JSON data file, performs literal placeholder
 * replacements, and writes the assembled prompt to an output file.
 *
 * This replaces the previous awk/sed approach which corrupted JSON when PR
 * titles or bodies contained special characters like '&'.
 *
 * Usage:
 *   node dist/assemble-prompt.js --template prompt-template.md --data /tmp/release-data.json --output /tmp/claude-prompt.md
 */

import * as fs from 'fs';

interface Args {
  template: string;
  data: string;
  output: string;
}

function parseArgs(argv: string[]): Args {
  const args: Partial<Args> = {};

  for (let i = 2; i < argv.length; i++) {
    switch (argv[i]) {
      case '--template':
        args.template = argv[++i];
        break;
      case '--data':
        args.data = argv[++i];
        break;
      case '--output':
        args.output = argv[++i];
        break;
      default:
        process.stderr.write(`Unknown argument: ${argv[i]}\n`);
        process.exit(1);
    }
  }

  if (!args.template || !args.data || !args.output) {
    process.stderr.write(
      'Usage: assemble-prompt --template <path> --data <path> --output <path>\n'
    );
    process.exit(1);
  }

  return args as Args;
}

function main(): void {
  const args = parseArgs(process.argv);

  // Read inputs
  const template = fs.readFileSync(args.template, 'utf-8');
  const dataRaw = fs.readFileSync(args.data, 'utf-8');
  const data = JSON.parse(dataRaw);

  // Extract metadata fields from the JSON
  const fromTag: string = data.fromTag ?? '';
  const toTag: string = data.toTag ?? '';
  const repo: string = data.repo ?? '';

  // Literal string replacements (replaceAll is literal, not regex, in JS/TS)
  let result = template;
  result = result.replaceAll('__RELEASE_DATA__', dataRaw.trim());
  result = result.replaceAll('__FROM_TAG__', fromTag);
  result = result.replaceAll('__TO_TAG__', toTag);
  result = result.replaceAll('__REPO__', repo);

  // Write output
  fs.writeFileSync(args.output, result, 'utf-8');

  // Log summary to stderr
  process.stderr.write(
    `Prompt assembled: ${args.output} (${Buffer.byteLength(result)} bytes)\n`
  );
  process.stderr.write(`  Range: ${fromTag} → ${toTag}\n`);
  process.stderr.write(`  Repo: ${repo}\n`);
}

main();
