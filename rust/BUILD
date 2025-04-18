# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.


load("@rules_rust//rust:defs.bzl", "rust_library", "rust_test", "rustfmt_test")
load("@typedb_bazel_distribution//github:rules.bzl", "deploy_github")
load("@typedb_bazel_distribution//crates:rules.bzl", "assemble_crate", "deploy_crate")
load("@typedb_dependencies//distribution:deployment.bzl", "deployment")
load("@typedb_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("@typedb_dependencies//tool/release/deps:rules.bzl", "release_validate_deps")
load("//:deployment.bzl", deployment_github = "deployment")

rust_library(
    name = "typeql",
    srcs = glob([
        "*.rs",
        "**/*.rs",
    ], exclude=["target/**"]),
    crate_root = "typeql.rs",
    compile_data = [
        "parser/typeql.pest",
    ],
    deps = [
        "@crates//:chrono",
        "@crates//:itertools",
        "@crates//:pest",
        "@crates//:regex",
    ],
    proc_macro_deps = [
        "@crates//:pest_derive",
    ],
    visibility = ["//visibility:public"],
    tags = ["crate-name=typeql"]
)

rust_test(
    name = "typeql_unit_tests",
    crate = ":typeql",
    deps = [
        "@crates//:syn",
        "@crates//:proc-macro2",
    ],
)

assemble_crate(
    name = "assemble_crate",
    target = ":typeql",
    description = "TypeQL Language for Rust",
    readme_file = "//:README.md",
    license = "Apache-2.0",
    license_file = "//:LICENSE",
    homepage = "https://github.com/typedb/typeql",
    repository = "https://github.com/typedb/typeql",
    keywords = ["typeql", "typedb", "database", "strongly-typed"],
    authors = ["TypeDB <community@typedb.com>"]
)

deploy_crate(
    name = "deploy_crate",
    target = ":assemble_crate",
    snapshot = deployment["crate"]["snapshot"],
    release = deployment["crate"]["release"],
)

deploy_github(
    name = "deploy_github",
    release_description = "//:RELEASE_NOTES_LATEST.md",
    title = "TypeQL",
    title_append_version = True,
    organisation = deployment_github['github.organisation'],
    repository = deployment_github['github.repository'],
    draft = False
)

rustfmt_test(
    name = "rustfmt_test",
    targets = [":typeql"],
    size = "small",
)

filegroup(
    name = "rustfmt_config",
    srcs = ["rustfmt.toml"],
)

checkstyle_test(
    name = "checkstyle",
    include = glob([
        "*",
        "*/**",
      ], exclude = [
        "target/**",
        "README.md",
        "**/Cargo.toml",
        "**/Cargo.lock",
    ]),
    exclude = ["Cargo.toml"],
    license_type = "mpl-header",
)
