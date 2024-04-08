# Package CLI Project GitHub Action

This GitHub Action is designed to package the dotCMS CLI as an NPM project.

## Inputs

### `branch`

- **Description**: Branch to build from
- **Required**: No
- **Default**: 'master'

### `github-token`

- **Description**: GitHub Token
- **Required**: Yes

### `npm-package-name`

- **Description**: NPM package name
- **Required**: No
- **Default**: 'dotcli'

### `npm-package-scope`

- **Description**: NPM package scope
- **Required**: No
- **Default**: '@dotcms'

## Runs

This action runs using a composite strategy.

## Steps

1. **Checkout**
    - Checks out the repository at the specified branch.

2. **Download Build Artifact**
    - Downloads the build artifact from the specified workflow run on GitHub.

3. **Get SHAs and Check If We Should Deploy**
    - Retrieves the SHA of the artifact and checks if it should be deployed.

4. **Download All Build Artifacts**
    - Downloads all build artifacts necessary for packaging.

5. **List CLI Artifacts**
    - Lists all CLI artifacts downloaded.

6. **Extract Package Version**
    - Extracts the package version from the project.

7. **Dynamic Configuration of NPM Package Version and Tag**
    - Dynamically configures the NPM package version and tag based on Maven versioning.

8. **Install Jinja2**
    - Installs the Jinja2 CLI tool.

9. **NPM Package Setup**
    - Sets up the NPM package, including creating the bin folder with binaries, adding the postinstall.js script, and generating the package.json file using Jinja2.

10. **NPM Package Tree**
    - Lists the contents of the NPM package directory.

11. **Upload NPM Package Artifact**
    - Uploads the NPM package artifact.

### Usage

To use this GitHub Actions workflow, you can include the following step in your workflow YAML file:

```yaml
- name: Package CLI Project
  id: cli_deploy
  uses: ./.github/actions/package-npm-cli
  with:
    branch: master
    github-token: ${{ secrets.GITHUB_TOKEN }}
    npm-package-name: ${{ inputs.npm-package-name }}
    npm-package-scope: ${{ inputs.npm-package-scope }}
```

This will trigger the workflow to package the CLI project as an NPM package using the specified branch, GitHub token,
package name, and scope.

Remember to replace the uses field with the appropriate repository and branch where this action is located, and adjust other input values as needed.