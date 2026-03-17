#!/usr/bin/env node

/**
 * gather-release-data
 *
 * Gathers structured release data from GitHub for changelog generation.
 * Outputs JSON to stdout suitable for consumption by Claude or other AI models.
 *
 * Usage:
 *   npx ts-node src/index.ts --repo dotCMS/core --to-tag v26.03.13-02
 *   npx ts-node src/index.ts --repo dotCMS/core --from-tag v26.03.13-01 --to-tag v26.03.13-02
 */

import { CLIArgs, ReleaseData } from './types';
import {
  createOctokit,
  parseRepo,
  listStandardReleaseTags,
  findPreviousTag,
  fetchCommitRange,
  extractPRNumbers,
  fetchPRDetails,
} from './github';
import { processChanges } from './categorize';

/** Parse CLI arguments. */
function parseArgs(argv: string[]): CLIArgs {
  const args: Partial<CLIArgs> = {};

  for (let i = 2; i < argv.length; i++) {
    switch (argv[i]) {
      case '--repo':
        args.repo = argv[++i];
        break;
      case '--from-tag':
        args.fromTag = argv[++i];
        break;
      case '--to-tag':
        args.toTag = argv[++i];
        break;
      default:
        process.stderr.write(`Unknown argument: ${argv[i]}\n`);
        process.exit(1);
    }
  }

  if (!args.toTag) {
    process.stderr.write('Error: --to-tag is required.\n');
    process.stderr.write(
      'Usage: gather-release-data --repo owner/repo --to-tag vYY.MM.DD-NN [--from-tag vYY.MM.DD-NN]\n'
    );
    process.exit(1);
  }

  if (!args.repo) {
    process.stderr.write('Warning: --repo not specified, defaulting to dotCMS/core\n');
  }

  return {
    repo: args.repo || 'dotCMS/core',
    fromTag: args.fromTag,
    toTag: args.toTag,
  };
}

async function main(): Promise<void> {
  const args = parseArgs(process.argv);
  const { owner, repo } = parseRepo(args.repo);

  process.stderr.write(`Gathering release data for ${args.repo}...\n`);

  const octokit = createOctokit();

  // Resolve from-tag if not provided
  let fromTag = args.fromTag;
  if (!fromTag) {
    process.stderr.write(
      `No --from-tag provided, auto-detecting previous release...\n`
    );
    const tags = await listStandardReleaseTags(octokit, owner, repo);
    // Note: if this tag was just published, GitHub's release API may not yet
    // reflect it due to eventual consistency. A simple re-run of this workflow
    // will succeed once the API catches up (typically within seconds).
    fromTag = findPreviousTag(tags, args.toTag);
    if (!fromTag) {
      process.stderr.write(
        `Error: Could not find a previous standard release tag before ${args.toTag}\n`
      );
      process.exit(1);
    }
    process.stderr.write(`Resolved previous tag: ${fromTag}\n`);
  }

  process.stderr.write(`Comparing: ${fromTag}...${args.toTag}\n`);

  // Fetch commit range
  const { totalCommits, commits } = await fetchCommitRange(
    octokit,
    owner,
    repo,
    fromTag,
    args.toTag
  );

  process.stderr.write(`Found ${totalCommits} commits in range.\n`);

  if (commits.length < totalCommits) {
    process.stderr.write(
      `Warning: GitHub capped the commit response at ${commits.length} of ${totalCommits} total. ` +
        `Release notes will reflect only analyzed commits.\n`
    );
  }

  if (totalCommits === 0) {
    const emptyResult: ReleaseData = {
      repo: args.repo,
      fromTag,
      toTag: args.toTag,
      totalCommits: 0,
      rollbackUnsafe: [],
      skipped: [],
      changes: [],
    };
    process.stdout.write(JSON.stringify(emptyResult, null, 2) + '\n');
    return;
  }

  // Extract PR numbers from commit messages
  const prNumbers = extractPRNumbers(commits);
  process.stderr.write(
    `Extracted ${prNumbers.length} PR numbers from ${commits.length} commits.\n`
  );

  // Fetch PR details
  const prDetails = await fetchPRDetails(octokit, owner, repo, prNumbers);
  process.stderr.write(`Fetched details for ${prDetails.size} PRs.\n`);

  // Process and categorize
  const { changes, rollbackUnsafe, skipped } = processChanges(prDetails);

  // Build output — use commits.length (actual analyzed) not totalCommits (GitHub's
  // API-reported figure, which may exceed what the compare endpoint returns).
  const result: ReleaseData = {
    repo: args.repo,
    fromTag,
    toTag: args.toTag,
    totalCommits: commits.length,
    rollbackUnsafe,
    skipped,
    changes,
  };

  // Output JSON to stdout
  process.stdout.write(JSON.stringify(result, null, 2) + '\n');

  process.stderr.write(
    `Done. ${changes.length} changes, ${rollbackUnsafe.length} rollback-unsafe, ${skipped.length} skipped.\n`
  );
}

main().catch((error) => {
  process.stderr.write(`Fatal error: ${error.message}\n`);
  process.exit(1);
});
