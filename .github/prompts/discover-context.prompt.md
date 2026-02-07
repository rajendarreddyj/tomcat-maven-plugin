# Context Discovery Prompt Template
# Save as: .github/prompts/discover-context.prompt.md
# In Copilot Chat:
# #prompt:discover-context
# My search query: "How do we handle API rate limiting for third-party services?"
You are helping me find relevant context artifacts in our repository.

Our context is organized as:
- `.context/domains/{domain}/research/` - Research artifacts
- `.context/domains/{domain}/plans/` - Implementation plans
- Each domain has a DOMAIN.md file indexing its content

When I describe what I'm looking for, you should:
1. Identify relevant domains based on my description
2. Read the DOMAIN.md files for those domains
3. Suggest 2-3 most relevant artifacts
4. Explain why each artifact matches my query

My search query: [ENGINEER FILLS IN]
Example: "How do we handle API rate limiting for third-party services?"

Your response should be:
1. Relevant domains: [List 1-3 domains]
2. Recommended artifacts:
   - [Domain]/[Type]/[Filename]
     - Why: [1-2 sentence relevance explanation]
     - Last Updated: [Date from file]
     - Status: [Current/Archive/etc]
