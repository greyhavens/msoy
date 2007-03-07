Here lie the gory innards required for Swiftly's function (other than the code,
of course):

projects/
    - Local subversion repository storage. Will be replaced by remote storage.

templates/
    - Project templates. See templates/README.txt for more information.

flex_sdk/
    - A symlink to the current flex SDK. When the server packages are built,
      this symlink is resolved and the SDK is copied into the server package.

whirled_sdk/
    - Our local SDK, contains a lib/ symlink to the built whirled SDK
    (../dist/sdk/whirled_sdk/lib). This will need to be revisited when
    the SDK moves to its own repository. This too will be resolved and
    copied in the server package.
