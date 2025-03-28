/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

use std::fmt;

use super::definable::type_::CapabilityBase;
use crate::{
    common::{identifier::Identifier, token, Span, Spanned},
    pretty::Pretty,
    schema::definable::type_::capability::Relates,
    type_::Label,
};

#[derive(Debug, Eq, PartialEq)]
pub enum Undefinable {
    Type(Label),                                // undefine person;
    AnnotationType(AnnotationType),             // undefine @independent from name;
    AnnotationCapability(AnnotationCapability), // undefine @card from person owns name;
    CapabilityType(CapabilityType),             // undefine owns name from person;
    Specialise(Specialise),                     // undefine as parent from fathership relates father;

    Function(Function), // undefine fun reachable;
    Struct(Struct),     // undefine struct coords;
}

impl Pretty for Undefinable {}

impl fmt::Display for Undefinable {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Type(inner) => fmt::Display::fmt(inner, f),
            Self::AnnotationType(inner) => fmt::Display::fmt(inner, f),
            Self::AnnotationCapability(inner) => fmt::Display::fmt(inner, f),
            Self::CapabilityType(inner) => fmt::Display::fmt(inner, f),
            Self::Specialise(inner) => fmt::Display::fmt(inner, f),
            Self::Function(inner) => fmt::Display::fmt(inner, f),
            Self::Struct(inner) => fmt::Display::fmt(inner, f),
        }
    }
}

#[derive(Debug, Clone, Eq, PartialEq)]
pub struct AnnotationType {
    pub span: Option<Span>,
    pub annotation_category: token::Annotation,
    pub type_: Label,
}

impl AnnotationType {
    pub fn new(span: Option<Span>, annotation_category: token::Annotation, type_: Label) -> Self {
        Self { span, annotation_category, type_ }
    }
}

impl Spanned for AnnotationType {
    fn span(&self) -> Option<Span> {
        self.span
    }
}

impl Pretty for AnnotationType {}

impl fmt::Display for AnnotationType {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "@{} {} {}", self.annotation_category, token::Keyword::From, self.type_)
    }
}

#[derive(Debug, Clone, Eq, PartialEq)]
pub struct AnnotationCapability {
    pub span: Option<Span>,
    pub annotation_category: token::Annotation,
    pub type_: Label,
    pub capability: CapabilityBase,
}

impl AnnotationCapability {
    pub fn new(
        span: Option<Span>,
        annotation_category: token::Annotation,
        type_: Label,
        capability: CapabilityBase,
    ) -> Self {
        Self { span, annotation_category, type_, capability }
    }
}

impl Spanned for AnnotationCapability {
    fn span(&self) -> Option<Span> {
        self.span
    }
}

impl Pretty for AnnotationCapability {}

impl fmt::Display for AnnotationCapability {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "@{} {} {} {}", self.annotation_category, token::Keyword::From, self.type_, self.capability)
    }
}

#[derive(Debug, Clone, Eq, PartialEq)]
pub struct CapabilityType {
    pub span: Option<Span>,
    pub capability: CapabilityBase,
    pub type_: Label,
}

impl CapabilityType {
    pub fn new(span: Option<Span>, capability: CapabilityBase, type_: Label) -> Self {
        Self { span, capability, type_ }
    }
}

impl Spanned for CapabilityType {
    fn span(&self) -> Option<Span> {
        self.span
    }
}

impl Pretty for CapabilityType {}

impl fmt::Display for CapabilityType {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{} {} {}", self.capability, token::Keyword::From, self.type_)
    }
}

#[derive(Debug, Clone, Eq, PartialEq)]
pub struct Specialise {
    pub span: Option<Span>,
    pub specialised: Label,
    pub type_: Label,
    pub capability: Relates,
}

impl Specialise {
    pub fn new(span: Option<Span>, specialised: Label, type_: Label, relates: Relates) -> Self {
        Self { span, specialised, type_, capability: relates }
    }
}

impl Spanned for Specialise {
    fn span(&self) -> Option<Span> {
        self.span
    }
}

impl Pretty for Specialise {}

impl fmt::Display for Specialise {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(
            f,
            "{} {} {} {} {}",
            token::Keyword::As,
            self.specialised,
            token::Keyword::From,
            self.type_,
            self.capability
        )
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Function {
    pub span: Option<Span>,
    pub ident: Identifier,
}

impl Function {
    pub fn new(span: Option<Span>, ident: Identifier) -> Self {
        Self { span, ident }
    }
}

impl Spanned for Function {
    fn span(&self) -> Option<Span> {
        self.span
    }
}

impl Pretty for Function {}

impl fmt::Display for Function {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{} {}", token::Keyword::Fun, self.ident)
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Struct {
    pub span: Option<Span>,
    pub ident: Identifier,
}

impl Struct {
    pub fn new(span: Option<Span>, ident: Identifier) -> Self {
        Self { span, ident }
    }
}

impl Spanned for Struct {
    fn span(&self) -> Option<Span> {
        self.span
    }
}

impl Pretty for Struct {}

impl fmt::Display for Struct {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{} {}", token::Keyword::Struct, self.ident)
    }
}
