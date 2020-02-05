# asciidoc2zendesk
Asciidoc to zendesk converter & uploader

## Library structure
Documents directory structure should conform to following rules:
1. Library should be two levels deep 

    1.1. First level contains a list of categories

    1.2. Second level contains a list of sections

2. Directory naming does not matter 

3. Each directory should contain `.properties` file where category or section parameters should be set 
according to following examples:

* category-level `.properties` file
```
CATEGORY_TITLE=Category 2 title
CATEGORY_OLD_TITLE=... // [optional parameter]
CATEGORY_DESCRIPTION=Category 2 description
CATEGORY_POSITION=1
```

* section-level `.properties` file
```
SECTION_TITLE=Section 2.2 title
SECTION_OLD_TITLE=... // [optional parameter]
SECTION_DESCRIPTION=Section Section 2.2 description
SECTION_POSITION=1
```

NOTE: directories with no `.properties` file or with incorrect `.properties` syntax will be skipped. 

NOTE: if category with `CATEGORY_OLD_TITLE` exists, it will be renamed to `CATEGORY_TITLE`

NOTE: if section with `SECTION_OLD_TITLE` exists, it will be renamed to `SECTION_TITLE`

## Publishing to Zendesk
To publish a document to zendesk server its meta-parameters should be known.
Parser takes these parameters from comments in input file:

```
// :ZENDESK-TITLE:      <text>               // article title [mandatory header]
// :ZENDESK-ORDER:      <number>             // article sorting position 
// :ZENDESK-DRAFT:      {true|false}         // create article as a draft
// :ZENDESK-PROMOTED:   {true|false}         // set 'promoted' flag for an article    
// :ZENDESK-HIDDEN:     {true|false}         // if true, artibcle will be skipped from publishing
// :ZENDESK-TAGS:       <CSV list of labels> // comma-separated list of article labels
// :ZENDESK-OLD-TITLE:  <text>               // previous article title to be used for article renaming [optional parameter]
```

- If document with `ZENDESK-TITLE` title already exists, it will be overwritten
- If document with `ZENDESK-OLD-TITLE` title exists, it will be renamed to `ZENDESK-TITLE` 

Also zendesk server credentials should be provisioned to program:

```
  --url=https://<domain>.zendesk.com --user=<login> --token=<configured-access-token>
```

## Cleaning up stale articles
It can happen that zendesk server will contain articles which are not in repository already (so-called 'stale' articles).
You can remove them from zendesk server during publication, if you run program with `--clean` argument. 

## Converting documents
The data for program should be be provisioned like this:
    
- directory (`--dir=<directory>`)
    
    Directory will be processed recursively. All the *.adoc and *.asciidoc files will be converted (and published to 
    zendesk server). Subdirectories are being processed in such a way that at first all files from a directory are being 
    processed, then (if needed) all the 'stale' articles for given section are being removed and then all the 
    subdirectories are being processed. 
    
## Tagging documents
If `ZENDESK-TAGS` header is set, then article will be marked with these labels on zendesk server. Tags 
should be separated with commas.


## Table Of Contents
To insert TOC macro in resulting document use following syntax:

```
// ...

= Title
:hardbreaks:
:toc:

{zwsp}

... the rest of document ...
```
